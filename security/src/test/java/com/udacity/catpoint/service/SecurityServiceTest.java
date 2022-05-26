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
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.awt.image.BufferedImage;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class SecurityServiceTest {

    @Mock
    private ImageService imageService;

    @Mock
    private SecurityRepository securityRepository;

    @Mock
    private StatusListener statusListener;

    @Mock
    private SecurityService securityService;

    private Sensor sensor;

    private final String randomString = UUID.randomUUID().toString();


    private Sensor getSensor() {
        return new Sensor(randomString, SensorType.DOOR);
    }


    private Set<Sensor> getAllSensors (int count, boolean active) {
        String randomString = UUID.randomUUID().toString();
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
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void changeAlarmStatus_alarmArmedAndSensorActivated_alarmStatusPending(ArmingStatus armingStatus) {
        when(securityService.getSensors()).thenReturn(getAllSensors(2, true));
        when(securityService.getArmingStatus()).thenReturn(armingStatus);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test two
    @Test
    void changeAlarmStatus_alarmAlreadyPendingAndSensorActivated_alarmStatusAlarm() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);
    }

    // Test three
    @Test
    void changeAlarmStatus_alarmPendingAndAllSensorsInactive_changeToNoAlarm() {
        Set<Sensor> sensors = getAllSensors(2, false);
        when(securityService.getSensors()).thenReturn(sensors);
        securityService.setAlarmStatus(AlarmStatus.PENDING_ALARM);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    //Test four
    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void changeAlarmState_alarmActivateAndSensorStateChange_stateNotAffected() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test five
    @Test
    void changeAlarmState_systemActivatedWhileAlreadyActiveAndAlarmPending_changeToAlarmState() {
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        securityService.changeSensorActivationStatus(sensor, true);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // Test six
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void changeAlarmState_sensorDeactivateWithInactive_noChangeToAlarmState(AlarmStatus alarmStatus) {
        when(securityService.getAlarmStatus()).thenReturn(alarmStatus);
        securityService.changeSensorActivationStatus(sensor, false);
        verify(securityRepository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    // Test seven
    @Test
    void changeAlarmState_imageContainingCatDetectedAndSystemArmed_changeToAlarmStatus() {
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);

    }

    // Test eight
    @Test
    void changeAlarmState_noCatImageIdentifiedAndSensorsAreInActive_changeToAlarmStatus() {
        Set<Sensor> inActiveSensors = getAllSensors(2, false);
        when(securityService.getSensors()).thenReturn(inActiveSensors);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(false);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.NO_ALARM);

    }

    // Test nine
    @Test
    void changeAlarmStatus_systemDisArmed_changeToAlarmStatus() {
        securityService.setArmingStatus(ArmingStatus.DISARMED);
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    // Test Ten case 1
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void updateSensors_systemArmed_deactivateAllSensors(ArmingStatus armingStatus) {
       Set<Sensor> senors = getAllSensors(2, true);
       when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
       when(securityService.getSensors()).thenReturn(senors);
       securityService.setArmingStatus(armingStatus);
       securityService.getSensors().forEach(s -> {
           assertFalse(s.getActive());
       });
    }

    // Test Ten case 2
    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_AWAY", "ARMED_HOME"})
    void updateSensors_systemArmed_deactivateAllSensorsArmed(ArmingStatus armingStatus) {
        Set<Sensor> senors = getAllSensors(2, true);
        when(securityService.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        when(securityService.getSensors()).thenReturn(senors);
        securityService.setArmingStatus(armingStatus);
        securityService.getSensors().forEach(s -> {
            assertFalse(s.getActive());
        });
    }

    // Test eleven
    @Test
    void changeAlarmStatus_systemArmedHomeAndCatDetected_changeToAlarmStatus() {
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(securityService.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        securityService.processImage(mock(BufferedImage.class));
        verify(securityRepository, atMost(2)).setAlarmStatus(AlarmStatus.ALARM);

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
