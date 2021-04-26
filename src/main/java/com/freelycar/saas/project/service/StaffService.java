package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.*;
import com.freelycar.saas.exception.BatchDeleteException;
import com.freelycar.saas.jwt.TokenAuthenticationUtil;
import com.freelycar.saas.project.entity.*;
import com.freelycar.saas.project.model.StaffInfo;
import com.freelycar.saas.project.repository.*;
import com.freelycar.saas.util.UpdateTool;
import com.freelycar.saas.wechat.model.WeChatStaff;
import com.freelycar.saas.wxutils.WechatTemplateMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.*;

import static com.freelycar.saas.basic.wrapper.ResultCode.RESULT_DATA_NONE;

@Service
@Transactional(rollbackFor = Exception.class)
public class StaffService {
    private Logger logger = LoggerFactory.getLogger(StaffService.class);

    @Autowired
    private StaffRepository staffRepository;

    @Autowired
    private ArkRepository arkRepository;

    @Autowired
    private EmployeeRepository employeeRepository;

    @Autowired
    private ConsumerProjectInfoService consumerProjectInfoService;

    @Autowired
    private ReminderRepository reminderRepository;

    private RspStaffStoreRepository rspStaffStoreRepository;
    private StoreRepository storeRepository;

    @Autowired
    public void setRspStaffStoreRepository(RspStaffStoreRepository rspStaffStoreRepository) {
        this.rspStaffStoreRepository = rspStaffStoreRepository;
    }

    @Autowired
    public void setStoreRepository(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * 新增/修改员工对象
     *
     * @param staff
     * @return
     */
    public ResultJsonObject modify(Staff staff) {
        String phone = staff.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return ResultJsonObject.getErrorResult(null, "手机号必填，用于作为员工唯一编号");
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        try {
            //验重
            if (this.checkRepeatPhone(staff)) {
                return ResultJsonObject.getErrorResult(null, "已包含手机号为：“" + phone + "”的数据，不能重复添加。");
            }
            //是否有ID，判断时新增还是修改
            String id = staff.getId();
            if (StringUtils.isEmpty(id)) {
                staff.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                staff.setCreateTime(currentTime);
            } else {
                Optional<Staff> optional = staffRepository.findById(id);
                //判断数据库中是否有该对象
                if (!optional.isPresent()) {
                    logger.error("修改失败，原因：" + Staff.class + "中不存在id为 " + id + " 的对象");
                    return ResultJsonObject.getErrorResult(null);
                }
                Staff source = optional.get();
                //将目标对象（projectType）中的null值，用源对象中的值替换
                UpdateTool.copyNullProperties(source, staff);
            }


            //如果在employee表中查询不到手机号，则视为第一次录入员工，则员工保存成功后需要在employee表中生成一条数据
            Employee employee = getEmployeeByPhone(phone);
            if (null == employee) {
                Employee newEmployee = new Employee();
                newEmployee.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                newEmployee.setTrueName(staff.getName());
                newEmployee.setNotification(false);
                newEmployee.setPhone(phone);
                newEmployee.setAccount(phone);
                newEmployee.setPassword(staff.getPassword());
                newEmployee.setCreateTime(currentTime);
                employeeRepository.save(newEmployee);
            }
            //如果已有数据，则统一其智能柜登录账户密码
            else {
                String account = employee.getAccount();
                String password = employee.getPassword();
                if (StringUtils.hasText(account)) {
                    staff.setAccount(account);
                    staff.setPassword(password);
                    if (null != password) {
                        staff.setIsArk(true);
                    }
                    staff.setOpenId(employee.getOpenId());
                }
            }

            //执行保存/修改
            return ResultJsonObject.getDefaultResult(staffRepository.saveAndFlush(staff));
        } catch (Exception e) {
//            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null, e.getMessage());
        }
    }

    /**
     * 新增/修改服务商员工对象
     *
     * @param staff
     * @return
     */
    public ResultJsonObject modifyRspStaff(Staff staff) {
        String phone = staff.getPhone();
        if (StringUtils.isEmpty(phone)) {
            return ResultJsonObject.getErrorResult(null, "手机号必填，用于作为员工唯一编号");
        }
        Timestamp currentTime = new Timestamp(System.currentTimeMillis());
        try {
            //验重
            if (this.checkRepeatRspStaffPhone(staff)) {
                return ResultJsonObject.getErrorResult(null, "已包含手机号为：“" + phone + "”的数据，不能重复添加。");
            }
            //是否有ID，判断时新增还是修改
            String id = staff.getId();
            if (StringUtils.isEmpty(id)) {
                //新增服务商技师
                staff.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                staff.setCreateTime(currentTime);
                staff.setIsArk(true);//默认开通智能柜功能
                //技师服务的门店
            } else {
                Optional<Staff> optional = staffRepository.findById(id);
                //判断数据库中是否有该对象
                if (!optional.isPresent()) {
                    logger.error("修改失败，原因：" + Staff.class + "中不存在id为 " + id + " 的对象");
                    return ResultJsonObject.getErrorResult(null);
                }
                Staff source = optional.get();
                //将目标对象（projectType）中的null值，用源对象中的值替换
                UpdateTool.copyNullProperties(source, staff);
            }


            //如果在employee表中查询不到手机号，则视为第一次录入员工，则员工保存成功后需要在employee表中生成一条数据
            Employee employee = getEmployeeByPhone(phone);
            if (null == employee) {
                Employee newEmployee = new Employee();
                newEmployee.setDelStatus(Constants.DelStatus.NORMAL.isValue());
                newEmployee.setDefaultStaffId(staff.getId());
                newEmployee.setTrueName(staff.getName());
                newEmployee.setNotification(false);
                newEmployee.setPhone(phone);
                newEmployee.setAccount(phone);
                newEmployee.setPassword(staff.getPassword());
                newEmployee.setCreateTime(currentTime);
                employeeRepository.save(newEmployee);
            } else {
                //如果已有数据，则统一其智能柜登录账户密码
                String account = employee.getAccount();
                if (StringUtils.hasText(account)) {
                    employee.setPassword(staff.getPassword());
                    employeeRepository.saveAndFlush(employee);
                    staff.setIsArk(true);
                    staff.setOpenId(employee.getOpenId());
                }
            }
            Staff staffNew = staffRepository.saveAndFlush(staff);
            List<Store> storeList = staff.getStores();
            rspStaffStoreRepository.deleteByStaffId(staffNew.getId());
            List<RspStaffStore> staffStoreList = new ArrayList<>();
            for (Store store : storeList) {
                RspStaffStore staffStore = new RspStaffStore(staffNew.getId(), store.getId());
                staffStoreList.add(staffStore);
            }
            rspStaffStoreRepository.saveAll(staffStoreList);
            //执行保存/修改
            return ResultJsonObject.getDefaultResult(getRspStaff(staffNew.getId()));
        } catch (Exception e) {
//            e.printStackTrace();
            return ResultJsonObject.getErrorResult(null);
        }
    }

    public Staff getRspStaff(String staffId) {
        Staff result = null;
        Optional<Staff> staffOptional = staffRepository.findById(staffId);
        if (staffOptional.isPresent()) {
            result = staffOptional.get();
            Set<String> storeIds = rspStaffStoreRepository.findByStaffId(staffId);
            List<Store> storeList = storeRepository.findByDelStatusAndIdIn(Constants.DelStatus.NORMAL.isValue(), new ArrayList<>(storeIds));
            result.setStores(storeList);
        }
        return result;
    }


    /**
     * 验证员工是否重复（手机号唯一）
     * true：重复；false：不重复
     *
     * @param staff
     * @return
     */
    private boolean checkRepeatPhone(Staff staff) {
        List<Staff> staffList;
        if (null != staff.getId()) {
            staffList = staffRepository.checkRepeatPhone(staff.getId(), staff.getPhone(), staff.getStoreId());
        } else {
            staffList = staffRepository.checkRepeatPhone(staff.getPhone(), staff.getStoreId());
        }
        return staffList.size() != 0;
    }

    /**
     * 验证服务商员工是否重复（手机号唯一）
     *
     * @param staff
     * @return
     */
    private boolean checkRepeatRspStaffPhone(Staff staff) {
        List<Staff> staffList;
        if (null != staff.getId()) {
            staffList = staffRepository.checkRepeatRspStaffPhone(staff.getId(), staff.getPhone(), staff.getRspId());
        } else {
            staffList = staffRepository.checkRepeatRspStaffPhone(staff.getPhone(), staff.getRspId());
        }
        return staffList.size() != 0;
    }

    /**
     * 验证员工是否重复（员工姓名唯一）
     * true：重复；false：不重复
     * 注：不太合理，弃用
     *
     * @param staff
     * @return
     */
    @Deprecated
    private boolean checkRepeatName(Staff staff) {
        List<Staff> staffList;
        if (null != staff.getId()) {
            staffList = staffRepository.checkRepeatName(staff.getId(), staff.getName(), staff.getStoreId());
        } else {
            staffList = staffRepository.checkRepeatName(staff.getName(), staff.getStoreId());
        }
        return staffList.size() != 0;
    }

    /**
     * 获取员工详情
     *
     * @param id
     * @return
     */
    public ResultJsonObject getDetail(String id) {
        return ResultJsonObject.getDefaultResult(this.findById(id));
    }

    /**
     * 查询员工智能柜服务状态
     *
     * @param employeeId
     * @return
     */
    public boolean isArk(String employeeId) {
        Employee employee = employeeRepository.findById(employeeId).orElse(null);
        if (null != employee && employee.getDelStatus() == false) {
            String phone = employee.getPhone();
            List<Staff> staffList = staffRepository.findAllByPhoneAndDelStatus(phone, Constants.DelStatus.NORMAL.isValue());
            for (Staff staff :
                    staffList) {
                if (staff.getIsArk()) {
                    return true;
                }
            }
        }
        return false;
        /*Staff staff = this.findById(id);
        if (null == staff) {
            return false;
        }
        Boolean isArk = staff.getIsArk();
        return isArk == null ? false : isArk;*/
    }


    /**
     * 查询员工列表
     *
     * @param storeId
     * @param currentPage
     * @param pageSize
     * @return
     */
    public PaginationRJO list(String storeId, Integer currentPage, Integer pageSize, String id, String name) {
        logger.debug("storeId:" + storeId);
        Page<Staff> staffPage = staffRepository.findAllByDelStatusAndStoreIdAndIdContainingAndNameContaining(Constants.DelStatus.NORMAL.isValue(), storeId, id, name, PageableTools.basicPage(currentPage, pageSize));
        return PaginationRJO.of(staffPage);
    }

    /**
     * 查询服务商下员工列表
     */

    public PaginationRJO list(String rspId, String storeName, String staffName, Integer currentPage, Integer pageSize) {
        Page<Staff> staffPage = null;
        if (StringUtils.isEmpty(storeName)) {
            staffPage = staffRepository.findAllByDelStatusAndRspIdAndNameContaining(Constants.DelStatus.NORMAL.isValue(), rspId, staffName, PageableTools.basicPage(currentPage, pageSize));
            for (Staff staff :
                    staffPage.getContent()) {
                staff = getRspStaff(staff.getId());
            }
        } else {
            List<Store> storeList = storeRepository.findByNameContainingAndDelStatus(storeName, Constants.DelStatus.NORMAL.isValue());
            Set<String> storeIds = new HashSet<>();
            for (Store store : storeList) {
                storeIds.add(store.getId());
            }
            Set<String> staffIds = rspStaffStoreRepository.findByStoreIdIn(storeIds);
            staffPage = staffRepository.findAllByDelStatusAndRspIdAndNameContainingAndIdIn(Constants.DelStatus.NORMAL.isValue(), rspId, staffName, staffIds, PageableTools.basicPage(currentPage, pageSize));
            for (Staff staff :
                    staffPage.getContent()) {
                staff = getRspStaff(staff.getId());
            }
        }
        return PaginationRJO.of(staffPage);
    }

    /**
     * 删除操作（软删除）
     *
     * @param id
     * @return
     */
    @Transactional
    public ResultJsonObject delete(String id) {
        try {
            int result = staffRepository.delById(id);
            if (result != 1) {
                return ResultJsonObject.getErrorResult(id, "删除失败," + RESULT_DATA_NONE);
            }
        } catch (Exception e) {
            return ResultJsonObject.getErrorResult(id, "删除失败，删除操作出现异常");
        }
        return ResultJsonObject.getDefaultResult(id, "删除成功");
    }

    public ResultJsonObject delete(String[] ids) throws BatchDeleteException {
        Set<String> idSet = new HashSet<>(Arrays.asList(ids));
        int result = staffRepository.delById(idSet);
        if (result == 0) {
            return ResultJsonObject.getErrorResult(ids, "删除失败," + RESULT_DATA_NONE);
        }
        if (result != ids.length) {
            throw new BatchDeleteException("部分id不存在");
        }
        return ResultJsonObject.getDefaultResult(ids, "删除成功");
    }


    /**
     * 智能柜技师开通
     *
     * @param id
     * @return
     */

    public ResultJsonObject openArk(String id, String account, String password) {
        Optional<Staff> optionalStaff = staffRepository.findById(id);
        if (optionalStaff.isPresent()) {
            Staff staff = optionalStaff.get();
            Staff staffResult = null;

            //开通的时候查询employee表中的数据
            Employee employee = employeeRepository.findTopByPhoneAndDelStatus(staff.getPhone(), Constants.DelStatus.NORMAL.isValue());
            if (null != employee) {
                //设置所有staff及其employee账号密码为这次开通的值
                List<Staff> staffs = staffRepository.findAllByPhoneAndDelStatus(account, Constants.DelStatus.NORMAL.isValue());
                if (null != staffs && !staffs.isEmpty()) {
                    for (Staff s : staffs) {
                        s.setAccount(account);
                        s.setPassword(password);
                    }
                    staffRepository.saveAll(staffs);
                }

                //保存自己的数据
                staff.setAccount(account);
                staff.setPassword(password);
                staff.setIsArk(true);
                staffResult = staffRepository.save(staff);

                //同步employee数据
                employee.setAccount(account);
                employee.setPassword(password);
                employeeRepository.save(employee);

                return ResultJsonObject.getDefaultResult(staffResult);
            } else {
                ResultJsonObject.getErrorResult(null, "查询不到对应的employee表数据，开通失败");
            }

        }
        return ResultJsonObject.getErrorResult(null, "id:" + id + "不存在，开通失败");


    }

    /**
     * 验证账户是否重复
     * true：重复，false：不重复
     *
     * @param staff
     * @return
     */

    private boolean checkRepeatAccount(Staff staff) {
        List<Staff> staffList;
        if (StringUtils.isEmpty(staff.getId())) {
            staffList = staffRepository.checkRepeatAccount(staff.getAccount());
        } else {
            staffList = staffRepository.checkRepeatAccount(staff.getAccount(), staff.getId());
        }
        return staffList.size() != 0;
    }

    /**
     * 智能柜技师开通
     *
     * @param id
     * @return
     */
    public ResultJsonObject openArk(String id) {

        Optional<Staff> optionalStaff = staffRepository.findById(id);
        if (optionalStaff.isPresent()) {
            Staff staff = optionalStaff.get();
            staff.setIsArk(true);
            staffRepository.saveAndFlush(staff);
            return ResultJsonObject.getDefaultResult(getRspStaff(id));
        }
        return ResultJsonObject.getErrorResult(null, "id:" + id + "不存在！");

    }

    /**
     * 智能柜技师关闭
     *
     * @param id
     * @return
     */
    public ResultJsonObject closeArk(String id) {
        Optional<Staff> optionalStaff = staffRepository.findById(id);
        if (optionalStaff.isPresent()) {
            Staff staff = optionalStaff.get();
            staff.setIsArk(false);
            staffRepository.saveAndFlush(staff);
            return ResultJsonObject.getDefaultResult(getRspStaff(id));
        }
        return ResultJsonObject.getErrorResult(null, "id:" + id + "不存在！");

    }

    /**
     * 批量删除
     *
     * @param ids
     * @return
     */
    public ResultJsonObject delByIds(String ids) {
        if (StringUtils.isEmpty(ids)) {
            return ResultJsonObject.getErrorResult(null, "删除失败：ids" + ResultCode.PARAM_NOT_COMPLETE.message());
        }
        String[] idsList = ids.split(",");
        for (String id : idsList) {
            staffRepository.delById(id);
        }
        return ResultJsonObject.getDefaultResult(null);
    }


    /**
     * 技师端登录
     *
     * @param account
     * @param password
     * @return
     */
    public ResultJsonObject login(String account, String password, String openId, String unionId) {
        if (StringUtils.isEmpty(account)) {
            logger.error("登录失败，参数account为空！");
            return ResultJsonObject.getErrorResult(null, "登录失败，参数account为空！");
        }
        if (StringUtils.isEmpty(password)) {
            logger.error("登录失败，参数password为空！");
            return ResultJsonObject.getErrorResult(null, "登录失败，参数password为空！");
        }
        Staff staff = staffRepository.findTopByAccountAndPasswordAndDelStatus(account, password, Constants.DelStatus.NORMAL.isValue());
        if (null != staff) {
            //更新openId到数据库（用于推送消息）
            staff.setOpenId(openId);
            staff.setUnionId(unionId);
            modify(staff);
            String jwt = TokenAuthenticationUtil.generateAuthentication(staff.getId());
            return ResultJsonObject.getDefaultResult(new WeChatStaff(jwt, staff));
        }

        return ResultJsonObject.getErrorResult(null, ResultCode.USER_LOGIN_ERROR.message());
    }

    /**
     * 技师登出（清除openId）
     *
     * @param staffId
     * @return
     */
    public ResultJsonObject logout(String staffId) {
        //清除openId，这样就不会给技师推送了
        Staff staff = staffRepository.findById(staffId).orElse(null);
        if (null == staff) {
            return ResultJsonObject.getErrorResult(staffId, "登出失败，对应的人员信息为查询到");
        }
        //暂时不清除openId
//        staff.setOpenId(null);
//        staffRepository.save(staff);
        logger.info("技师" + staff.getName() + "退出登录");
        return ResultJsonObject.getDefaultResult(staffId);
    }

    /**
     * 获取门店的所有有智能柜帐号的员工
     *
     * @param storeId
     * @return
     */
    public List<Staff> getAllArkStaffInStore(String storeId) {
        return staffRepository.findAllByDelStatusAndIsArkAndStoreId(Constants.DelStatus.NORMAL.isValue(), true, storeId);
    }

    /**
     * 获取服务商下所有有智能柜账号的员工
     *
     * @param rspId
     * @return
     */
    public List<Staff> getAllArkStaffInRsp(String rspId) {
        return staffRepository.findAllByDelStatusAndIsArkAndRspId(Constants.DelStatus.NORMAL.isValue(), true, rspId);
    }

    /**
     * 获取服务商下对某个门店的全部可接单技师
     *
     * @param rspId
     * @return
     */
    private List<Staff> staffReady(String rspId, String storeId) {
        List<Staff> staffList = getAllArkStaffInRsp(rspId);
        List<Staff> resList = new ArrayList<>();
        for (Staff staff :
                staffList) {
            Set<String> storeIds = rspStaffStoreRepository.findByStaffId(staff.getId());
            if (!storeIds.contains(storeId)) {
                continue;
            } else {
                String phone = staff.getPhone();
                Employee employee = getEmployeeByPhone(phone);
                boolean notification = (null == employee.getNotification()) ? false : employee.getNotification();
                if (notification) {
                    resList.add(staff);
                }
            }
        }
        return resList;
    }

    @Autowired
    private RSPStoreService rspStoreService;

    /**
     * 根据技师id获取全部服务门店
     *
     * @param staffId
     * @return
     */
    public List<Store> findServicingStoreByStaffId(String staffId) {
        Staff staff = staffRepository.findById(staffId).orElse(null);
        if (null != staff) {
            String rspId = staff.getRspId();
            List<Store> storeList = rspStoreService.listStore(rspId);
            return storeList;
        }
        return null;
    }

    /**
     * 给所有技师推送微信消息
     * （0：有用户预约了订单，通知该智能柜所有技师）
     * （1：有技师接单了，通知其余所有技师）
     * （4：有用户取消了订单，通知所有技师）
     *
     * @param consumerOrder
     * @param door
     * @param exceptOpenId  已接单技师openId
     */
    public void sendWeChatMessageToStaff(ConsumerOrder consumerOrder, Door door, String exceptOpenId) {
        String storeId = consumerOrder.getStoreId();
        Integer state = consumerOrder.getState();

        //查询门店的地址
        Ark ark = arkRepository.findTopBySnAndDelStatus(door.getArkSn(), Constants.DelStatus.NORMAL.isValue());
        List<Staff> allStaffList = this.getAllArkStaffInStore(storeId);
        logger.info("查询到storeId为" + storeId + "的门店有" + allStaffList.size() + "个技师");

        //汽车服务项目通知特别字段
        String projects = "汽车服务";
        StringBuilder projectStr = new StringBuilder();
        List<String> projectIds = new ArrayList<>();
        //查询项目
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(consumerOrder.getId());
        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            String projectName = consumerProjectInfo.getProjectName();
            if (StringUtils.hasText(projectName)) {
                projectStr.append("，").append(projectName);
            }
            projectIds.add(consumerProjectInfo.getProjectId());
        }
        if (StringUtils.hasText(projectStr)) {
            projects = projectStr.substring(1, projectStr.length());
        }

        // 筛选包含项目的技师
        List<Staff> staffList = new ArrayList<>();
        for (Staff staffObject : allStaffList) {
            boolean carryThisProject = false;
            List<Project> staffProjects = staffObject.getProjects();
            for (Project project : staffProjects) {
                if (projectIds.contains(project.getId())) {
                    carryThisProject = true;
                    break;
                }
            }
            if (carryThisProject) {
                staffList.add(staffObject);
            }
        }
        logger.info("包含项目：" + projects + "的技师有：" + staffList.size() + "个");

        // 遍历符合条件的技师，给他们发送信息
        for (Staff staff : staffList) {
            //openId来源变更，采用employee表中的openId
            String phone = staff.getPhone();
            if (StringUtils.hasText(phone)) {
                Employee employee = getEmployeeByPhone(phone);
                if (null != employee) {
                    boolean notification = employee.getNotification();
                    String openId = employee.getOpenId();

                    logger.info("技师openId：" + openId);
                    if (notification && StringUtils.hasText(openId)) {
                        if (state == 0) {
                            WechatTemplateMessage.orderCreated(consumerOrder, projects, openId, door, ark);
                        }
                        if (state == 4) {
                            WechatTemplateMessage.orderChangedForStaff(consumerOrder, openId, door, ark);
                        }
                        if (state == 1 && !openId.equals(exceptOpenId)) {
                            WechatTemplateMessage.orderChangedForStaff(consumerOrder, openId, door, ark);
                        }
                    }
                }
            }

        }
    }

    /**
     * 给所有技师推送微信消息(新)
     * （0：有用户预约了订单，通知该智能柜所有技师），所有技师：接单提醒
     * （1：有技师接单了，通知其余所有技师）,单个接单技师取车提醒
     * （4：有用户取消了订单，通知所有技师）
     *
     * @param consumerOrder
     * @param door
     * @param exceptOpenId  已接单技师openId
     * @param rspId         服务商id
     */
    public void sendWeChatMessageToStaff(ConsumerOrder consumerOrder, Door door, String exceptOpenId, String rspId) {
        String storeId = consumerOrder.getStoreId();
        Integer state = consumerOrder.getState();

        //查询门店的地址
        Ark ark = arkRepository.findTopBySnAndDelStatus(door.getArkSn(), Constants.DelStatus.NORMAL.isValue());
        List<Staff> allStaffList = this.getAllArkStaffInStore(storeId);
        logger.info("查询到storeId为" + storeId + "的门店有" + allStaffList.size() + "个技师");

        //汽车服务项目通知特别字段
        String projects = "汽车服务";
        StringBuilder projectStr = new StringBuilder();
        List<String> projectIds = new ArrayList<>();
        //查询项目
        List<ConsumerProjectInfo> consumerProjectInfos = consumerProjectInfoService.getAllProjectInfoByOrderId(consumerOrder.getId());
        for (ConsumerProjectInfo consumerProjectInfo : consumerProjectInfos) {
            String projectName = consumerProjectInfo.getProjectName();
            if (StringUtils.hasText(projectName)) {
                projectStr.append("，").append(projectName);
            }
            projectIds.add(consumerProjectInfo.getProjectId());
        }
        if (StringUtils.hasText(projectStr)) {
            projects = projectStr.substring(1, projectStr.length());
        }
        // 筛选包含项目的技师
        List<Staff> staffList = staffReady(rspId, storeId);
        logger.info("包含项目：" + projects + "的技师有：" + staffList.size() + "个");
        // 遍历符合条件的技师，给他们发送信息
        //更新：添加消息提醒
        List<Reminder> reminderList = new ArrayList<>();
        Set<String> employeeIds = new HashSet<>();
        for (Staff staff : staffList) {
            //openId来源变更，采用employee表中的openId
            String phone = staff.getPhone();
            if (StringUtils.hasText(phone)) {
                Employee employee = getEmployeeByPhone(phone);
                String employeeId = employee.getId();

                if (null != employee) {
                    boolean notification = employee.getNotification();
                    String openId = employee.getOpenId();
                    logger.info("技师openId：" + openId);
                    if (notification && StringUtils.hasText(openId)) {
                        if (state == Constants.OrderState.RESERVATION.getValue()) {
                            WechatTemplateMessage.orderCreated(consumerOrder, projects, openId, door, ark);

                            //添加消息提醒：技师：接单提醒
                            Reminder reminder = new Reminder();
                            reminder.setToEmployee(employeeId);
                            reminder.setType(Constants.MessageType.EMPLOYEE_ORDER_RECEIVING_REMINDER.getType());
                            reminder.setMessage(Constants.MessageType.EMPLOYEE_ORDER_RECEIVING_REMINDER.getMessage());

                            if (!employeeIds.contains(employeeId)) {
                                reminderList.add(reminder);
                                employeeIds.add(employeeId);
                            }
                        }
                        if (state == Constants.OrderState.CANCEL.getValue()) {
                            Reminder reminder = new Reminder();
                            reminder.setToEmployee(employeeId);
                            reminder.setType(Constants.MessageType.CLIENT_CANCEL_REMINDER.getType());
                            reminder.setMessage(Constants.MessageType.CLIENT_CANCEL_REMINDER.getMessage());
                            reminderList.add(reminder);
                            WechatTemplateMessage.orderChangedForStaff(consumerOrder, openId, door, ark);
                        }
                        if (state == Constants.OrderState.ORDER_TAKING.getValue()) {
                            //添加消息提醒：技师：取车提醒
                            if (staff.getId().equals(consumerOrder.getPickCarStaffId())) {
                                Reminder reminder = new Reminder();
                                reminder.setToEmployee(employeeId);
                                reminder.setType(Constants.MessageType.EMPLOYEE_PICK_UP_REMINDER.getType());
                                reminder.setMessage(Constants.MessageType.EMPLOYEE_PICK_UP_REMINDER.getMessage());
                                reminderList.add(reminder);
                            }
                            if (!openId.equals(exceptOpenId)) {
                                WechatTemplateMessage.orderChangedForStaff(consumerOrder, openId, door, ark);
                            }
                        }
                    }
                }
            }
        }

        if (reminderList.size() > 0) {
            reminderRepository.saveAll(reminderList);
        }
    }

    public Staff findById(String id) {
        return staffRepository.findById(id).orElse(null);
    }

    public List<Staff> findByPhone(String phone) {
        return staffRepository.findAllByPhoneAndDelStatus(phone, Constants.DelStatus.NORMAL.isValue());
    }

    public List<Staff> findByPhoneAndIsArk(String phone, boolean isArk) {
        return staffRepository.findAllByPhoneAndDelStatusAndIsArk(phone, Constants.DelStatus.NORMAL.isValue(), true);
    }

    public List<Staff> findByPhoneAndRspId(String phone, String rspId) {
        return staffRepository.findByPhoneAndDelStatusAndIsArkAndRspId(phone, Constants.DelStatus.NORMAL.isValue(), true, rspId);
    }

    /**
     * 根据手机号，查出接单店员的头像、手机、姓名等信息
     *
     * @param staffId
     * @return
     */
    public StaffInfo findStaffInfoForOrderByStaffId(String staffId) {
        StaffInfo staffInfo = new StaffInfo();
        staffInfo.setStaffId(staffId);
        if (StringUtils.hasText(staffId)) {
            Staff staffObject = this.findById(staffId);
            if (null != staffObject) {
                String staffName = staffObject.getName();
                staffInfo.setStaffName(staffName);
                String phone = staffObject.getPhone();
                if (StringUtils.hasText(phone)) {
                    staffInfo.setPhone(phone);
                    Employee employee = getEmployeeByPhone(phone);
                    if (null != employee) {
                        staffInfo.setEmployeeId(employee.getId());
                        staffInfo.setHeadImgUrl(employee.getHeadImgUrl());
                    }
                }
            }
        }
        return staffInfo;
    }

    public Employee getEmployeeByPhone(String phone) {
        return employeeRepository.findTopByPhoneAndDelStatus(phone, Constants.DelStatus.NORMAL.isValue());
    }
}
