package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.basic.wrapper.ResultJsonObject;
import com.freelycar.saas.exception.ArgumentMissingException;
import com.freelycar.saas.exception.DataIsExistException;
import com.freelycar.saas.exception.ObjectNotFoundException;
import com.freelycar.saas.jwt.TokenAuthenticationUtil;
import com.freelycar.saas.project.entity.Employee;
import com.freelycar.saas.project.entity.Staff;
import com.freelycar.saas.project.repository.EmployeeRepository;
import com.freelycar.saas.project.repository.StaffRepository;
import com.freelycar.saas.project.repository.StoreRepository;
import com.freelycar.saas.util.TimestampUtil;
import com.freelycar.saas.util.UpdateTool;
import com.freelycar.saas.wechat.model.EmployeeInfo;
import com.freelycar.saas.wechat.model.WeChatEmployee;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import javax.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;

/**
 * @author tangwei - Toby
 * @date 2019-06-17
 * @email toby911115@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class EmployeeService {
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private ConsumerOrderService consumerOrderService;

    public Employee modify(Employee employee) throws EntityNotFoundException, ArgumentMissingException, DataIsExistException {
        if (null == employee) {
            throw new ArgumentMissingException("参数employee对象为null");
        }
        //验重
        if (checkRepeatPhone(employee)) {
            throw new DataIsExistException("已存在手机号码为 " + employee.getPhone() + " 的雇员信息");
        }
        String id = employee.getId();
        if (StringUtils.isEmpty(id)) {
            employee.setCreateTime(TimestampUtil.getCurrentTimestamp());
            employee.setDelStatus(Constants.DelStatus.NORMAL.isValue());
        } else {
            Employee source = employeeRepository.getOne(id);
            //将目标对象中的null值，用源对象中的值替换
            UpdateTool.copyNullProperties(source, employee);
        }
        return employeeRepository.saveAndFlush(employee);
    }

    private boolean checkRepeatPhone(Employee employee) {
        List<Employee> employeeList;
        String id = employee.getId();
        String agentId = employee.getAgentId();
        String phone = employee.getPhone();
        if (null != id) {
            employeeList = employeeRepository.checkRepeatPhone(id, agentId, phone);
        } else {
            employeeList = employeeRepository.checkRepeatPhone(agentId, phone);
        }
        return employeeList.size() != 0;
    }

    public ResultJsonObject selectStore(Employee employee) throws ArgumentMissingException, ObjectNotFoundException, DataIsExistException {
        if (null == employee) {
            throw new ArgumentMissingException("操作失败，参数对象employee为空");
        }
        String id = employee.getId();
        String defaultStoreId = employee.getDefaultStoreId();
//        String defaultStaffId = employee.getDefaultStaffId();

        if (StringUtils.isEmpty(id)) {
            throw new ArgumentMissingException("操作失败，参数对象employee中id为空");
        }
        if (StringUtils.isEmpty(defaultStoreId)) {
            throw new ArgumentMissingException("操作失败，参数对象employee中defaultStoreId为空");
        }

        //查询employee对象，验证employee对象是否存在
        Employee employeeObj = employeeRepository.findById(id).orElse(null);
        if (null == employeeObj) {
            throw new ObjectNotFoundException("操作失败，未找到id为：" + id + " 的employee对象");
        }

        //拿到手机号
        String phone = employeeObj.getPhone();
        if (StringUtils.isEmpty(phone)) {
            throw new ObjectNotFoundException("操作失败，未找到id为：" + id + " 的employee对象中的手机号");
        }

        //通过手机和门店ID查对应的staff对象
        Staff staff = staffRepository.findTopByStoreIdAndPhoneAndDelStatusAndIsArk(defaultStoreId, phone, Constants.DelStatus.NORMAL.isValue(), true);

        if (null == staff) {
            throw new ObjectNotFoundException("操作失败，未找到手机号为：" + phone + " 的门店员工对象，可能该未给该店员开通智能柜权限");
        }

        //切换门店
        employee.setDefaultStaffId(staff.getId());

        Employee resultObj = null;

        try {
            resultObj = modify(employee);
        } catch (EntityNotFoundException e) {
            throw new ObjectNotFoundException("操作失败，未找到id为：" + id + " 的employee对象");
        }
        if (null != resultObj) {
            //查询门店名称
            Employee finalResultObj = resultObj;
            storeRepository.findById(defaultStoreId).ifPresent(storeObject -> finalResultObj.setDefaultStoreName(storeObject.getName()));
        }
        return ResultJsonObject.getDefaultResult(resultObj);
    }

    /**
     * 雇员登录方法（微信端技师登录方法）
     *
     * @param employee
     * @return
     */
    public ResultJsonObject login(Employee employee) {
        String account = employee.getAccount();
        String password = employee.getPassword();
        String openId = employee.getOpenId();

        if (StringUtils.isEmpty(account) || StringUtils.isEmpty(password)) {
            return ResultJsonObject.getErrorResult(null, "登录失败：接收到的参数中，用户名或密码为空");
        }
        if (StringUtils.isEmpty(openId)) {
            return ResultJsonObject.getErrorResult(null, "登录失败：接收到的参数中，openId为空。注意：这会影响消息推送");
        }

        // 查询是否有这个账号，没有的话直接返回登录失败
        Employee employeeResult = employeeRepository.findTopByAccountAndPasswordAndDelStatus(account, password, Constants.DelStatus.NORMAL.isValue());
        if (null == employeeResult) {
            return ResultJsonObject.getErrorResult(null, "不存在有效的账号，请核实后重新登录");
        }

        //更新微信的相关数据：头像、性别、省份、城市、昵称
        String headImgUrl = employee.getHeadImgUrl();
        String nickName = employee.getNickName();
        String gender = employee.getGender();
        String province = employee.getProvince();
        String city = employee.getCity();

        if (StringUtils.hasText(headImgUrl)) {
            employeeResult.setHeadImgUrl(headImgUrl);
        }
        if (StringUtils.hasText(nickName)) {
            employeeResult.setNickName(nickName);
        }
        if (StringUtils.hasText(gender)) {
            employeeResult.setGender(gender);
        }
        if (StringUtils.hasText(province)) {
            employeeResult.setProvince(province);
        }
        if (StringUtils.hasText(city)) {
            employeeResult.setCity(city);
        }
        if (StringUtils.hasText(openId)) {
            employeeResult.setOpenId(openId);
        }

        employeeRepository.save(employeeResult);


        //查询staff表中有几个对应的数据，列举出来供用户选择门店
        List<Staff> staffList;
        try {
            staffList = getStaffs(account);
        } catch (ArgumentMissingException | ObjectNotFoundException e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, "登录成功，但" + e.getMessage() + "，请联系管理人员");
        }

        String jwt = TokenAuthenticationUtil.generateAuthentication(employeeResult.getId());

        return ResultJsonObject.getDefaultResult(new WeChatEmployee(jwt, employeeResult, staffList));
    }

    /**
     * 根据手机号查询门店店员账号
     *
     * @param phone
     * @return
     * @throws ObjectNotFoundException
     * @throws ArgumentMissingException
     */
    public List<Staff> listStaffByPhone(String phone) throws ObjectNotFoundException, ArgumentMissingException {
        return getStaffs(phone);
    }

    /**
     * 根据手机号查询staff表中的有效数据
     *
     * @param phone
     * @return
     * @throws ArgumentMissingException
     */
    public List<Staff> getStaffs(String phone) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(phone)) {
            throw new ArgumentMissingException("手机号查询staff数据失败：参数phone为空值");
        }
        List<Staff> staffList = staffRepository.findAllByPhoneAndDelStatusAndIsArk(phone, Constants.DelStatus.NORMAL.isValue(), true);
        if (staffList.isEmpty()) {
            throw new ObjectNotFoundException("未查询到手机号为：" + phone + " 的staff信息");
        }
        for (Staff staff : staffList) {
            String storeId = staff.getStoreId();
            if (StringUtils.hasText(storeId)) {
                storeRepository.findById(storeId).ifPresent(storeObject -> staff.setStoreName(storeObject.getName()));
            }
        }
        return staffList;
    }


    /**
     * 展示微信端雇员的详细信息
     *
     * @param id
     * @return
     * @throws ArgumentMissingException
     */
    public ResultJsonObject detail(String id) throws ArgumentMissingException, ObjectNotFoundException {
        Employee employee = this.findObjectById(id);

        String defaultStaffId = employee.getDefaultStaffId();
        String defaultStoreId = employee.getDefaultStoreId();

        if (StringUtils.isEmpty(defaultStaffId)) {
            throw new ArgumentMissingException("参数defaultStaffId缺失，无法查询相关信息");
        }
        if (StringUtils.isEmpty(defaultStoreId)) {
            throw new ArgumentMissingException("参数defaultStoreId缺失，无法查询相关信息");
        }
//        Staff staff = staffRepository.getOne(defaultStaffId);
//        Store store = storeRepository.getOne(defaultStoreId);

        EmployeeInfo employeeInfo = new EmployeeInfo();
        employeeInfo.setName(employee.getTrueName());
        employeeInfo.setCity(employee.getCity());
        employeeInfo.setProvince(employee.getProvince());
        employeeInfo.setNotification(employee.getNotification());
        employeeInfo.setHeadImgUrl(employee.getHeadImgUrl());
        employeeInfo.setGender(employee.getGender());
        employeeInfo.setDefaultStaffId(employee.getDefaultStaffId());
        employeeInfo.setDefaultStoreId(employee.getDefaultStoreId());

        //查询历史订单条数
        int res = 0;
        try {
            res = consumerOrderService.getStaffOrderServiced(defaultStaffId);
        } catch (ArgumentMissingException e) {
            logger.error(e.getMessage(), e);
        }

        employeeInfo.setHistoryOrderCount(res);


        return ResultJsonObject.getDefaultResult(employeeInfo);
    }

    /**
     * 切换服务状态
     *
     * @param id
     * @return
     * @throws ArgumentMissingException
     */
    public ResultJsonObject switchServiceStatus(String id) throws ArgumentMissingException, ObjectNotFoundException {
        Employee employee = this.findObjectById(id);
        boolean notification = employee.getNotification();
        //置为相反状态
        employee.setNotification(!notification);
        try {
            employeeRepository.save(employee);
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null);
        }
        return ResultJsonObject.getDefaultResult(employee);
    }

    Employee findObjectById(String id) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(id)) {
            throw new ArgumentMissingException("参数id为空值");
        }
        Optional<Employee> employeeOptional = employeeRepository.findById(id);
        if (!employeeOptional.isPresent()) {
            throw new ObjectNotFoundException("未找到id为：" + id + "的employee数据");
        }
        return employeeOptional.get();
    }


    public List listStaffsForAgent(String agentId) throws ArgumentMissingException {
        if (StringUtils.isEmpty(agentId)) {
            throw new ArgumentMissingException();
        }


        return null;
    }
}
