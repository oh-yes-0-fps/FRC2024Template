# Constants

## Structure and syntax
All fields will be capitalized snake case <br>
All fileds will be public, static and _not_ final <br>
All classes will be public and static <br>
You store values similar to a nested hashmap <br>
```java
public class ConstValues {

    public static final boolean DEBUG = true; 

    public static class kExample {

        public static int VALUE = 2;

        @StringConst(yin = "yin", yang = "yang")
        public static String ROBOT_NAME;

        public static class kNested {
            public static int NESTED_CONST = 1;
        }
    }

    public static class kExample2 {
        public static int VALUE = 3;
    }
}
```

## Accessing constants
You can access constants via public fields <br>
```java
public class Example extends SubsystemBase {
    private String name = ConstValues.kExample.ROBOT_NAME;
    public Example() {
        System.out.println("Hello " + name);
    }
}
```

## Primitive constant mult-robot declaration
when needing to declare a primitive value differently depending on you can use one of the primitive const annotations
```java
public class ConstValues {
    //for this example robot names are yin and yang
    @IntConst(yin = 1, yang = 2)
    public int CONST_INT;
}
```

## Constants and NetworkTables
while debug is true all of constants is sent to NT <br>
by default values are mutable, but you can make them immutable by adding the annotation @TunableIgnore
you can also not send a value to NT with @NTIgnore
```java
public class ConstValues {
    @NTIgnore
    public static boolean VALUE = true;
    @TunableIgnore
    @StringConst(yin = "yin", yang = "yang")
    public static String NAME;
}
```