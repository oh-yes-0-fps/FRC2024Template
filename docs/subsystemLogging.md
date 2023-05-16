# Subsystem Logging

## Goals
1. Make the right way to the log also the easiest way to log. <br>
2. Abstract the repetitive/annoying code away while still allowing for all the freedom needed. <br>
3. Standardize the way we log across coders and subsystems. <br>
4. Quicker iteration cycles with tunable values and little overhead at competition due to the debug falg. <br>

## How to use
From ..util.AutoLog use the AL class <br>
This exposes the annotations used for logging <br>
The example below shows how to use the datalog annotation, sending the fields value to datalog every cycle <br>
```java
import com.igknighters.subsystems.Resources.McqSubsystemRequirements;
import com.igknighters.util.logging.AutoLog.AL;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Example extends SubsystemBase implements McqSubsystemRequirements{

    @AL.DataLog
    private double setpoint = 0.0;

    public Example() {}
}
```
<br>
The below example shows all logging 
<p> Supported Types(primitive or not): Double, Boolean, String, Integer,
Double[], Boolean[], String[], Integer[], Sendable

```java
import com.igknighters.subsystems.Resources.McqSubsystemRequirements;
import com.igknighters.util.logging.AutoLog.AL;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Example extends SubsystemBase implements McqSubsystemRequirements{

    //datalog the value of val1 every cycle
    @AL.DataLog
    private double val1 = 0.0;

    //datalog the value of val2 once on bootup
    @AL.DataLog(oneShot = true)
    private String val2 = 0.0;

    //log the value to SmartDashboard every cycle
    @AL.SmartDashboard
    private int val3 = 0;

    //log the value to SmartDashboard once on bootup
    @AL.SmartDashboard(oneShot = true)
    private boolean val4 = false;

    //Shuffleboard entries are sent to datalog if debug false
    //they keep the same path of NT/Shuffleboard/...

    //Puts the field onto Shuffleboard
    @Shuffleboard
    private Double val5 = 0.0;

    //Puts the field onto Shuffleboard with metadata
    //all of the metadata args are optional
    @Shuffleboard(pos = {1,1}, size = {2,1}, widget = "TextView")
    private String val6 = "";

    public Example() {}
}
```

## Tunable values
Allowing for runtime value iteration can drastically decrease development and testing time. <br>
For subsystems the best way to go about this is the Tunable annotation<br>

```java
import com.igknighters.subsystems.Resources.McqSubsystemRequirements;
import com.igknighters.util.logging.AutoLog.AL;
import edu.wpi.first.wpilibj2.command.SubsystemBase;

public class Example extends SubsystemBase implements McqSubsystemRequirements{

    //Tunable only supports primitive boolean and double

    //this posts the value to the TunableValues network table
    //changes to that NT entry will update the value of this field
    @AL.Tunable
    private double setpoint = 5.0;

    //it can used in combination with @AL.Shuffleboard & @AL.SmartDashboard to divert the entry instead

    @AL.Tunable
    @AL.SmartDashboad //the oneshot arg is ignored with tunable
    private boolean fullSpeed = false;

    @AL.Tunable
    @AL.Shuffleboard //metadata args do work here
    private double fallbackSetpoint = 10.0;

    public Example() {}
}
```