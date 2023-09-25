package commons;

import java.util.Arrays;
import java.util.List;

public enum InstanceEnum {

    SIMPLE("instance_simple"),
    LARGE("instance_large"),
    RANDOM_10("instance_random_10"),
    RANDOM_20("instance_random_20");

    private final Instance instance;

    InstanceEnum(String instanceName) {
        this.instance = new Instance(instanceName, true);
    }

    public static List<Instance> allInstances() {
        return Arrays.stream(values()).map(InstanceEnum::getInstance).toList();
    }

    public Instance getInstance() {
        return instance;
    }
}
