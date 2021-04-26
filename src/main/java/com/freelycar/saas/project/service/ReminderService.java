package com.freelycar.saas.project.service;

import com.freelycar.saas.basic.wrapper.Constants;
import com.freelycar.saas.project.entity.Reminder;
import com.freelycar.saas.project.repository.ReminderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author: pyt
 * @Date: 2021/4/21 10:45
 * @Description:
 */
@Service
@Transactional(rollbackFor = Exception.class)
public class ReminderService {
    private Logger logger = LoggerFactory.getLogger(ReminderService.class);

    private ReminderRepository reminderRepository;

    @Autowired
    public void setReminderRepository(ReminderRepository reminderRepository) {
        this.reminderRepository = reminderRepository;
    }

    public void createReminder() {
        Reminder reminder = new Reminder();
        reminder.setToClient("ea8ecbc57858c4160178c91c51ba04c8");
        reminder.setType(Constants.MessageType.CLIENT_PLACE_ORDER_REMINDER.getType());
        reminder.setMessage(Constants.MessageType.CLIENT_PLACE_ORDER_REMINDER.getMessage());
        reminderRepository.saveAndFlush(reminder);
    }


    /**
     * 根据clientId或employeeId获取未读消息
     * 并将消息置为已读
     *
     * @param id
     * @return
     */
    public List<Reminder> listById(String id) {
        List<Reminder> reminders = reminderRepository.findByToId(id);
        reminders = reminders.stream().parallel().map(reminder -> {
            reminder.setRead(true);
            return reminder;
        }).collect(Collectors.toList());
        reminderRepository.saveAll(reminders);
        return reminders;
    }

    public Integer count(String id) {
        return reminderRepository.countByToId(id);
    }
}
