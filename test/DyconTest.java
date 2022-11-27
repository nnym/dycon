import java.lang.invoke.MethodHandles;
import net.auoeke.dycon.Dycon;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.annotation.Testable;
import sun.misc.Unsafe;

@Testable
public class DyconTest {
	static int count;

	static Unsafe lazyUnsafe() {
		return Dycon.ldc(() -> {
			++count;
			return (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup()).findStaticGetter(Unsafe.class, "theUnsafe", Unsafe.class).invokeExact();
		});
	}

	static Unsafe lazyLazyUnsafe() {
		return Dycon.ldc(DyconTest::lazyUnsafe);
	}

	Integer count(Object o) {
		return Dycon.ldc(() -> count);
	}

	static <T> T allocateInstance(Class<T> type) {
		return (T) lazyUnsafe().allocateInstance(type);
	}

	@Test void test() {
		assert count == 0 : count;
		assert this.count(null) == 0;
		assert lazyUnsafe() != null;
		assert count == 1 : count;
		assert this.count(null) == 0;
		assert lazyUnsafe() == lazyUnsafe() && lazyLazyUnsafe() == lazyUnsafe();
		assert count == 1 : count;
		assert allocateInstance(Object.class) != allocateInstance(Object.class);
		assert count == 1 : count;
	}
}
