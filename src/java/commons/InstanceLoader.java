package commons;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class InstanceLoader {

  private static List<Instance> allInstances;
  private static InstanceLoader instance;

  private InstanceLoader() {
    allInstances = loadAllInstances();
  }

  private static List<Instance> loadAllInstances() {
    List<Instance> ret = new ArrayList<>();
    File resourcesDir = new File(Instance.DEFAULT_DIR);
    for (File dir : Objects.requireNonNull(resourcesDir.listFiles(File::isDirectory))) {
      if (dir.getName().startsWith("instance")) {
        ret.add(new Instance(dir.getName(), true));
      }
    }
    return ret;
  }

  public static InstanceLoader getInstance() {
    if (instance == null) {
      instance = new InstanceLoader();
    }
    return instance;
  }

  public List<Instance> getExperimentInstances() {
    return allInstances.stream()
        .filter(i -> i.getName().matches("instance_n[0-9]+_s[0-9]+_k[0-9]+"))
        .sorted(Comparator.comparing(Instance::getNumberOfNodes))
        .toList();
  }

  public List<Instance> getTestInstances() {
    return allInstances.stream()
        .filter(i -> !i.getName().matches("instance_n[0-9]+_s[0-9]+_k[0-9]+"))
        .sorted(Comparator.comparing(Instance::getNumberOfNodes))
        .toList();
  }
}
