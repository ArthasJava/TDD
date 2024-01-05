package arthas.tdd.di;

import org.junit.jupiter.api.Nested;

public class ContainerTest {
    @Nested
    public class DependenciesSelection {
        @Nested
        public class ProviderType {

        }

        @Nested
        public class Qualifier {

        }
    }

    @Nested
    public class LifecycleManagement {
        // TODO 默认新建
        // TODO 当 scope 是 singleton，只创建一个容器
    }

}

interface Component {
    default Dependency dependency() {
        return null;
    }
}

interface Dependency { }

interface AnotherDependency { }
