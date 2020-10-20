package com.freelycar.saas.project.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * @author tangwei - Toby
 * @date 2019-06-24
 * @email toby911115@gmail.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class HistoryOrder {
    private String id;

    private String clientName;

    private Date finishTime;

    private String licensePlate;

    private String carColor;

    private String carImageUrl;

    private String carBrand;

    private String payState;

    private String projectNames;
    private String keyLocation;
    private String parkingLocation;
}
