package arthas.tdd.di;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ContainerTest {
    private Context context;

    @BeforeEach
    void setUp() {
        context = new Context();
    }

    @Nested
    public class ComponentConstruction {
        // TODO instance
        @Test
        void should_bind_type_to_a_specific_instance() {
            Component instance = new Component() { };
            context.bind(Component.class, instance);

            assertSame(instance, context.get(Component.class));
        }

        // TODO abstract clas
        // TODO interface
        @Nested
        public class ConstructorInjection {
            // TODO no args constructor
            @Test
            void should_bind_type_to_a_class_with_default_constructor() {
                context.bind(Component.class, ComponentWithDefaultConstructor.class);

                Component actual = context.get(Component.class);
                assertNotNull(actual);
                assertInstanceOf(ComponentWithDefaultConstructor.class, actual);
            }
            // TODO with dependencies
            // TODO A --> B --> C
        }

        public class FieldInjection {

        }

        public class MethodInjection {

        }
    }

    @Nested
    public class DependenciesSelection {
        // TODO 通过标记选择
        // TODO
    }

    @Nested
    public class LifecycleManagement {
        // TODO 默认新建
        // TODO 当 scope 是 singleton，只创建一个容器
    }
}

interface Component { }

class ComponentWithDefaultConstructor implements Component {
    public ComponentWithDefaultConstructor() {
    }
}
