// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.Relay.Value;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.cameraserver.CameraServer;
import frc.robot.subsystems.IndexSubsystem;
import frc.robot.variables.Motors;
import frc.robot.variables.Objects;

public class Robot extends TimedRobot {
    /**
     * Main robot functions
     */
    DriveAndOperate driveAndOperate = new DriveAndOperate();
    Autonomous autonomous = new Autonomous();
    Test test = new Test();

    /**
     * Robot variables
     */
    public String autoSelected;

    @Override
    public void robotInit() {
        Objects.smartDashboardUpdater.setupSmartDashboard();
        CameraServer.startAutomaticCapture();
        
    }

    @Override
    public void robotPeriodic() {
    }

    @Override
    public void autonomousInit() {
        Objects.visionSubsystem.turnOnLeds();
        autoSelected = Objects.smartDashboardUpdater.getAutoSelected();
    
    }

    @Override
    public void autonomousPeriodic() {
        Objects.drivetrain.updateOdometry();
        switch (autoSelected) {
            case "Red Path":
                autonomous.redPath();
                break;
            case "Orange Path":
                autonomous.orangePath();
                break;
            case "Yellow Path":
                autonomous.yellowPath();
                break;
            case "Green Path":
                autonomous.greenPath();
                break;
            case "Blue Path":
                autonomous.bluePath();
                break;
            case "Purple Path":
                autonomous.purplePath();
                break;
            case "Default Path":
            default:
                autonomous.defaultPath();
                break;
        }
        
        Objects.visionSubsystem.updateVision();
        Objects.indexSubsystem.backgroundIndex();
    }

    @Override
    public void teleopInit() {
    
    }

    @Override
    public void teleopPeriodic() {
        driveAndOperate.readDriverController();
        driveAndOperate.readOperatorController();
        // driveAndOperate.testJoystickRead();
        driveAndOperate.driveAndOperate();       
        Objects.indexSubsystem.backgroundIndex();
        Objects.visionSubsystem.updateVision();
    }


}
