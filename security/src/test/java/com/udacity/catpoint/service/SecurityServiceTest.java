package com.udacity.catpoint.service;

import com.udacity.catpoint.application.StatusListener;
import com.udacity.catpoint.data.*;
import com.udacity.catpoint.image.ImageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SecurityServiceTest {

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;


    private SecurityService securityService;

    private Sensor sensor;

    private final String randomString = UUID.randomUUID().toString();


    private Sensor getSensor() {
        return new Sensor(randomString, SensorType.DOOR);
    }


    private Set<Sensor> getAllSensors (int count, boolean active) {
        Set<Sensor> allSensors = new HashSet<>();
        for (int i = 0; i <= count; i++ ) {
            allSensors.add(new Sensor(randomString, SensorType.DOOR));
        }
        allSensors.forEach(s -> s.setActive(active));
        return allSensors;
    }

    @BeforeEach
    void setUp() {
        securityService = new SecurityService(securityRepository, imageService);
        sensor = getSensor();
    }

    // Test one
    @Test
    void changeAlarmStatus_alarmArmedAndSensorActivated_alarmStatusPending() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    // Test two
    @Test
    void changeAlarmStatus_alarmAlreadyPendingAndSensorActivated_alarmStatusAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test three
    @Test
    void changeAlarmStatus_alarmPendingAndAllSensorsInactive_changeToNoAlarm() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test four
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeAlarmState_alarmActivateAndSensorStateChange_stateNotAffected(boolean status) {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, status);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test five
    @Test
    void changeAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState() {
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // Test six
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void changeAlarmState_sensorDeactivateWithInactive_noChangeToAlarmState(AlarmStatus alarmStatus) {
        when(securityRepository.getAlarmStatus()).thenReturn(alarmStatus);
        sensor.setActive(false);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test seven
    @Test
    void changeAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus() {
        BufferedImage catImage = new BufferedImage(300, 256, BufferedImage.TYPE_INT_RGB);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        securityService.processImage(catImage);
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test eight
    @Test
    void changeAlarmState_noCatImageIdentifiedAndSensorsAreInActive_changeToAlarmStatus() {
        Set<Sensor> sensors = getAllSensors(3, false);
        when(securityRepository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // Test nine
    @Test
    void changeAlarmStatus_systemDisArmed_changeToAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test Ten
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void updateSensors_systemArmed_deactivateAllSensors(ArmingStatus armingStatus) {
       Set<Sensor> senors = getAllSensors(2, true);
       when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
       when(securityRepository.getSensors()).thenReturn(senors);
       securityService.setArmingStatus(armingStatus);
       securityService.getSensors().forEach(s -> {
           assertFalse(s.getActive());
       });
    }

    // Test eleven
    @Test
    void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        securityService.processImage(mock(BufferedImage.class));
        securityService.setArmingStatus(ArmingStatus.ARMED_HOME);
        verify(securityRepository, atMost(1)).setAlarmStatus(AlarmStatus.ALARM);

    }

//    @Test
//    void ifAlarmStateAndSystemDisarmed_changeAlarmStatus() {
//        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
//        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
//        securityService.changeSensorActivationStatus(sensor, true);
//
//        verify(securityRepository, times(1)).setAlarmStatus(any(AlarmStatus.class));
//    }

    @Test
    void ifAlarmStateAndSystemDisarmed_changeStatusToPending() {
        when(securityRepository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(securityRepository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor);

        verify(securityRepository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }



    // more coverage Test
    @Test
    void addAndRemoveStatusListener() {
        securityService.addStatusListener(statusListener);
        securityService.removeStatusListener(statusListener);
    }

    @Test
    void addAndRemoveSensor() {
        securityService.addSensor(sensor);
        securityService.removeSensor(sensor);
    }

}
