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

	@Test void test() {
		assert count == 0 : count;
		assert lazyUnsafe() != null;
		assert count == 1 : count;
		assert lazyUnsafe() == lazyUnsafe();
		assert count == 1 : count;
	}
}
