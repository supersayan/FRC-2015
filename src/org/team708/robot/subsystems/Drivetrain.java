package org.team708.robot.subsystems;

import org.team708.robot.RobotMap;
import org.team708.robot.commands.drivetrain.JoystickDrive;

import edu.wpi.first.wpilibj.BuiltInAccelerometer;
import edu.wpi.first.wpilibj.CANTalon;
import edu.wpi.first.wpilibj.Gyro;
import edu.wpi.first.wpilibj.RobotDrive;
import edu.wpi.first.wpilibj.command.PIDSubsystem;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;

/**
 * This class is a drivetrain subsystem that uses PID to drive straight.
 * @author Nam Tran & Victor Lourng
 */
public class Drivetrain extends PIDSubsystem {
	
	// PID Tuning parameters
	private static final double Kp = 0.05;		// Proportional gain		// Was 0.05 for coulsons
	private static final double Ki = 0.01;		// Integral gain			// Was 0.01 for coulsons
	private static final double Kd = 0.0;		// Derivative gain
	private static final double tolerance = 5;
	
	// Variables specific for drivetrain PID loop
	private double moveSpeed = 0.0;
	private double pidOutput = 0.0;
	private static final double tankControlTolerance = .025;
	
	private CANTalon leftMaster, leftSlave1, leftSlave2, rightMaster, rightSlave1, rightSlave2;		// Motor Controllers
	private RobotDrive drivetrain;		// FRC provided drivetrain class
	
	private BuiltInAccelerometer accelerometer;		// Accelerometer that is built into the roboRIO
	private Gyro gyro;		// Gyro that is used for drift correction
	
	private boolean brake = true;		// Whether the talons should be in coast or brake mode (this could be important if a jerky robot causes things to topple
	
    /**
     * Constructor
     */
    public Drivetrain() {
    	// Passes variables from this class into the superclass constructor
    	super("Drivetrain", Kp, Ki, Kd);
    	
    	// Initializes motor controllers with device IDs from RobotMap
		leftMaster = new CANTalon(RobotMap.drivetrainLeftMotorMaster);
		leftSlave1 = new CANTalon(RobotMap.drivetrainLeftMotorSlave1);
		leftSlave2 = new CANTalon(RobotMap.drivetrainLeftMotorSlave2);
		rightMaster = new CANTalon(RobotMap.drivetrainRightMotorMaster);
		rightSlave1 = new CANTalon(RobotMap.drivetrainRightMotorSlave1);
		rightSlave2 = new CANTalon(RobotMap.drivetrainRightMotorSlave2);
		
		drivetrain = new RobotDrive(leftMaster, rightMaster);		// Initializes drivetrain class
		
		setupMasterSlave();			// Sets up master and slave
		
		accelerometer = new BuiltInAccelerometer();		// Initializes the accelerometer from the roboRIO
		gyro = new Gyro(RobotMap.gyro);					// Initializes the gyro
		gyro.reset();									// Resets the gyro so that it starts with a 0.0 value
    	
		setAbsoluteTolerance(tolerance);
		setInputRange(-360.0, 360);
        setSetpoint(0.0);
        disable();
    }
    
    /**
     * Initializes the default command for this subsystem
     */
    public void initDefaultCommand() {
        // Set the default command for a subsystem here.
        setDefaultCommand(new JoystickDrive());
    }
    
    /**
     * Drives the drivetrain using a forward-backward value and a rotation value
     * @param move
     * @param rotate
     */
    public void haloDrive(double move, double rotate) {
    	// Checks whether drift correction is needed
    	if (rotate == 0.0 && move != 0.0) {
    		// Enables the PID controller if it is not already
    		if (!getPIDController().isEnable()) {
    			gyro.reset();
    			enable();
    		}
    		// Sets the forward move speed to the move parameter
    		moveSpeed = move;
    	} else {
    		// Disables the PID controller if it enabled so the drivetrain can move freely
    		if (getPIDController().isEnable()) {
    			disable();
    		}
    		drivetrain.arcadeDrive(move, rotate);
    	}
    }
    
    /**
     * Drives the drivetrain using a left motor(s) value and a right motor(s) value
     * @param left
     * @param right
     */
    public void tankDrive(double left, double right) {
    	// Checks whether drift correction is needed
    	if (Math.abs(left - right) < tankControlTolerance && left != 0.0 && right != 0.0) {
    		// Enables the PID controller if it is not already
    		if (!getPIDController().isEnable()) {
    			enable();
    		}
    		// Sets the forward move speed to the average of the two sticks
    		moveSpeed = ((left + right) / 2);
    	} else {
    		// Disables the PID controller if it enabled so the drivetrain can move freely
    		if (getPIDController().isEnable()) {
    			disable();
    		}
    		drivetrain.tankDrive(left, right);
    	}
    }
    
    public void stop() {
    	leftMaster.set(0.0);
    	rightMaster.set(0.0);
    }
    
    /**
     * Gets the degrees that the gyro is reading
     * @return
     */
    public double getAngle() {
    	return gyro.getAngle();
    }
    
    /**
     * Resets the gyro reading
     */
    public void resetGyro() {
    	gyro.reset();
    }
    
    /**
     * Sets up the drivetrain motors to have a master that is controlled by the 
     * default FRC RobotDrive class and slaves that do whatever the master
     * talon is doing
     */
    public void setupMasterSlave() {
    	leftSlave1.changeControlMode(CANTalon.ControlMode.Follower);
		leftSlave2.changeControlMode(CANTalon.ControlMode.Follower);
		rightSlave1.changeControlMode(CANTalon.ControlMode.Follower);
		rightSlave2.changeControlMode(CANTalon.ControlMode.Follower);
		
		leftSlave1.set(leftMaster.getDeviceID());
		leftSlave2.set(leftMaster.getDeviceID());
		rightSlave1.set(rightMaster.getDeviceID());
		rightSlave2.set(rightMaster.getDeviceID());
    }
    
    /**
     * Toggles between brake and coast mode for the talons
     */
    public void toggleBrakeMode() {
    	brake = !brake;
    	leftMaster.enableBrakeMode(brake);
    	leftSlave1.enableBrakeMode(brake);
    	leftSlave2.enableBrakeMode(brake);
    	rightMaster.enableBrakeMode(brake);
    	rightSlave1.enableBrakeMode(brake);
    	rightSlave2.enableBrakeMode(brake);
    }
    
    /**
     * Returns a process variable to the PIDSubsystem for correction
     */
    protected double returnPIDInput() {
    	return gyro.getAngle();
    }
    
    /**
     * Performs actions using the robot to correct for any error using the outputed value
     */
    protected void usePIDOutput(double output) {
        pidOutput = output;
        drivetrain.arcadeDrive(moveSpeed, -output);
    }
    
    /**
     * Sends data for this subsystem to the dashboard
     */
    public void sendToDashboard() {
    	SmartDashboard.putNumber("Accelerometer X", accelerometer.getX());
    	SmartDashboard.putNumber("Accelerometer Y", accelerometer.getY());
    	SmartDashboard.putNumber("Accelerometer Z", accelerometer.getZ());
    	SmartDashboard.putNumber("Gyro angle", gyro.getAngle());
    	SmartDashboard.putNumber("Gyro Rate", gyro.getRate());
    	SmartDashboard.putBoolean("Brake", brake);
    	SmartDashboard.putNumber("PID Output", pidOutput);
    }
}
