package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.exception.*;
import com.freelycar.saas.iotcloudcn.ArkOperation;
import com.freelycar.saas.iotcloudcn.util.ArkThread;
import com.freelycar.saas.iotcloudcn.util.BoxCommandResponse;
import com.freelycar.saas.project.entity.Ark;
import com.freelycar.saas.project.entity.Door;
import com.freelycar.saas.project.repository.DoorRepository;
import com.freelycar.saas.util.cache.ConcurrentHashMapCacheUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author tangwei - Toby
 * @date 2019-02-18
 * @email toby911115@gmail.com
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class DoorService {
    private final static long TIME_INTERVAL = 5000;
    private final static long TIMEOUT = 50000;
    /**
     * 初始化一个存放正在操作的Door对象的缓存对象
     */
    private final static Map<String, List<String>> doorCacheVariable = new ConcurrentHashMap<>();
    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private DoorRepository doorRepository;

    public Door findById(String id) throws ArgumentMissingException, ObjectNotFoundException {
        if (StringUtils.isEmpty(id)) {
            throw new ArgumentMissingException("参数doorId为空");
        }
        Door door = doorRepository.findById(id).orElse(null);
        if (null == door) {
            throw new ObjectNotFoundException("未找到id为：" + id + " 的door对象");
        }
        return door;
    }

    /**
     * 获取一个随机的可使用的柜门
     *
     * @param arkSn
     * @return
     * @throws NoEmptyArkException
     * @throws ArgumentMissingException
     */
    public Door getUsefulDoor(String arkSn) throws NoEmptyArkException, ArgumentMissingException {
        if (StringUtils.isEmpty(arkSn)) {
            throw new ArgumentMissingException("参数arkSn缺失");
        }
        List<Door> emptyDoorList = doorRepository.findByArkSnAndStateAndDelStatus(arkSn, Constants.DoorState.EMPTY.getValue(), Constants.DelStatus.NORMAL.isValue());
        if (null == emptyDoorList || emptyDoorList.isEmpty()) {
            logger.error("没有可分配的智能柜！");
            throw new NoEmptyArkException("没有查找到可使用的空智能柜");
        }

        synchronized (doorCacheVariable) {
            return isOperatingDoor(emptyDoorList);
        }
    }

    private Door isOperatingDoor(List<Door> emptyDoors) throws NoEmptyArkException {
        if (null == emptyDoors || emptyDoors.isEmpty()) {
            logger.error("没有可分配的智能柜！");
            throw new NoEmptyArkException("没有查找到可使用的空智能柜");
        }
        int targetIndex;
        int emptyDoorsCount = emptyDoors.size();
        //获取一个随机下标
        Random random = new Random();
        targetIndex = random.nextInt(emptyDoorsCount);

        //得到door对象，去缓存对象里去查是否是被占用的柜子
        Door targetDoor = emptyDoors.get(targetIndex);
        String targetDoorId = targetDoor.getId();
//        String arkId = targetDoor.getArkId();

        //替换成封装的缓存工具类
        Door cacheDoor = (Door) ConcurrentHashMapCacheUtils.getCache(targetDoorId);
        if (null == cacheDoor) {
            ConcurrentHashMapCacheUtils.setCache(targetDoorId, targetDoor, ConcurrentHashMapCacheUtils.ONE_MINUTE);
            return targetDoor;
        }
        emptyDoors.remove(targetIndex);
        return isOperatingDoor(emptyDoors);
    }

    @Deprecated
    private void takeInDoorIdWithCache(Door door, List<String> cacheDoors) {
        logger.info("已分配的柜子存入缓存-----");
        cacheDoors.add(door.getId());
        doorCacheVariable.put(door.getArkId(), cacheDoors);
    }

    @Deprecated
    public void takeOutDoorIdWithCache(Door door) {
        logger.info("已分配的柜子从缓存中去除-----");
        //从缓存中去掉这个Door对象
        String arkId = door.getArkId();
        List<String> cacheDoors = doorCacheVariable.get(arkId);
        if (null != cacheDoors && !cacheDoors.isEmpty()) {
            for (String cacheDoorId : cacheDoors) {
                if (door.getId().equalsIgnoreCase(cacheDoorId)) {
                    cacheDoors.remove(cacheDoorId);
                    doorCacheVariable.put(arkId, cacheDoors);
                    break;
                }
            }
        }
    }


    /**
     * 重写开门方法
     *
     * @param door
     * @throws ArgumentMissingException
     * @throws OpenArkDoorFailedException
     * @throws OpenArkDoorTimeOutException
     * @throws InterruptedException
     */
    public void openDoorByDoorObject(Door door) throws ArgumentMissingException, OpenArkDoorFailedException, OpenArkDoorTimeOutException, InterruptedException {
        if (null == door) {
            throw new ArgumentMissingException("参数doorObject为空。");
        }
        String deviceId = door.getArkSn();
        int boxId = door.getDoorSn();

        if (StringUtils.isEmpty(deviceId)) {
            throw new ArgumentMissingException("参数doorObject中的arkSn值为空");
        }
        if (boxId < 1 || boxId > 16) {
            throw new ArgumentMissingException("参数boxId不是有效数字，无法开柜");
        }

        //打开柜门
        BoxCommandResponse boxCommandResponse = ArkOperation.openBox(deviceId, boxId);
        //判断是否成功，成功就启动监控线程
        if (null != boxCommandResponse && ArkOperation.SUCCESS_CODE == boxCommandResponse.code) {
            logger.info("arkOrderLog:开始执行智能柜开关门----------");
            logger.info("arkOrderLog:智能柜sn编号为：" + deviceId);
            logger.info("arkOrderLog:执行柜门号为：" + boxId + "号门");
            long start = System.currentTimeMillis();
            long end;
            String resState = "timeout";
            boolean startFlag = true;

            while (startFlag) {
                try {
                    Thread.sleep(TIME_INTERVAL);
                    //是否已经超时，超时则直接退出进程
                    if ((System.currentTimeMillis() - start) > TIMEOUT) {
                        logger.error(deviceId + " 柜" + boxId + "号门未关，已超时。线程终止。");
                        startFlag = false;
                        end = System.currentTimeMillis();
                        logger.info("完成智能柜开关任务------，耗时：" + (end - start) + "毫秒");
                        resState = "timeout";
                        break;
                    }
                } catch (InterruptedException e) {
                    logger.error(e.getMessage(), e);
                    e.printStackTrace();
                    throw new InterruptedException();
                }

                BoxCommandResponse response = ArkOperation.queryBox(deviceId, boxId);

                int code = response.code;
                boolean isOpen = response.is_open;
                if (ArkOperation.SUCCESS_CODE == code) {
                    if (!isOpen) {
                        logger.info(deviceId + " 柜" + boxId + "号门关闭。正常结束进程");
                        startFlag = false;
                        end = System.currentTimeMillis();
                        logger.info("完成智能柜开关任务------，耗时：" + (end - start) + "毫秒");
                        resState = "success";
                        break;
                    }
                }
            }

            //获取结果，如果不是success，说明超时
            if (!Constants.OPEN_SUCCESS.equalsIgnoreCase(resState)) {
                throw new OpenArkDoorTimeOutException("柜门关闭线程超时（50s）");
            }

            //如果正常到这边，不抛出异常，就说明一切正常，可以开单
        } else {
            throw new OpenArkDoorFailedException("打开柜门失败：从远端获取到打开柜门失败的信息。");
        }
    }

    /**
     * 打开柜门并启用监控线程
     *
     * @param door
     * @throws ArgumentMissingException
     * @throws OpenArkDoorFailedException
     * @throws OpenArkDoorTimeOutException
     */
    public void openDoorByDoorObjectOld(Door door) throws ArgumentMissingException, OpenArkDoorFailedException, OpenArkDoorTimeOutException {
        if (null == door) {
            throw new ArgumentMissingException("参数doorObject为空。");
        }
        String deviceId = door.getArkSn();
        int boxId = door.getDoorSn();

        if (StringUtils.isEmpty(deviceId)) {
            throw new ArgumentMissingException("参数doorObject中的arkSn值为空");
        }

        //打开柜门
        BoxCommandResponse boxCommandResponse = ArkOperation.openBox(deviceId, boxId);
        //判断是否成功，成功就启动监控线程
        if (null != boxCommandResponse && ArkOperation.SUCCESS_CODE == boxCommandResponse.code) {
            ArkThread arkThread = new ArkThread(deviceId, boxId);
            arkThread.start();

            //等待线程结束
            try {
                arkThread.join();
            } catch (InterruptedException e) {
                logger.error("捕获到等待线程中断异常……");
                e.printStackTrace();
            }

            String endStatus = arkThread.getEndStatus();
            //获取结果，如果不是success，说明超时
            if (!Constants.OPEN_SUCCESS.equalsIgnoreCase(endStatus)) {
                throw new OpenArkDoorTimeOutException("柜门关闭线程超时（50s）");
            }

            //如果正常到这边，不抛出异常，就说明一切正常，可以开单
        } else {
            throw new OpenArkDoorFailedException("打开柜门失败：从远端获取到打开柜门失败的信息。");
        }
    }

    /**
     * 生成智能柜的状态表数据（子表数据）
     *
     * @param ark
     * @throws ArgumentMissingException
     */
    public void generateDoors(Ark ark) throws ArgumentMissingException {
        if (null == ark) {
            throw new ArgumentMissingException("参数ark为空，生成door表数据失败");
        }
        String arkSn = ark.getSn();
        String arkId = ark.getId();
        String arkName = ark.getName();
        int doorNum = ark.getDoorNum();
        if (StringUtils.isEmpty(arkSn)) {
            throw new ArgumentMissingException("参数arkSn为空，生成door表数据失败");
        }
        for (int i = 0; i < doorNum; i++) {
            Door door = new Door();
            door.setDelStatus(Constants.DelStatus.NORMAL.isValue());
            door.setCreateTime(new Timestamp(System.currentTimeMillis()));
            door.setState(Constants.DoorState.EMPTY.getValue());
            door.setArkId(arkId);
            door.setArkSn(arkSn);
            door.setDoorSn(i + 1);
            door.setArkName(arkName);
            doorRepository.save(door);
        }
    }

    public List<Door> findAllDoorsByArkId(String arkId) {
        return doorRepository.findAllByArkIdAndDelStatus(arkId, Constants.DelStatus.NORMAL.isValue());
    }

    public boolean isDoorUsing(String arkId) throws ObjectNotFoundException {
        boolean result = false;
        List<Door> doors = findAllDoorsByArkId(arkId);
        if (doors.isEmpty()) {
            throw new ObjectNotFoundException("未找到arkId为：" + arkId + " 的door数据");
        }

        for (Door door : doors) {
            int state = door.getState();
            if (state == Constants.DoorState.USER_RESERVATION.getValue() || state == Constants.DoorState.STAFF_FINISH.getValue()) {
                result = true;
                break;
            }
        }
        return result;
    }


    public Door save(Door door) {
        return doorRepository.save(door);
    }

    public void deleteAllByArkId(String arkId) {
        List<Door> doors = findAllDoorsByArkId(arkId);
        for (Door door : doors) {
            doorRepository.delete(door);
        }
    }
}
