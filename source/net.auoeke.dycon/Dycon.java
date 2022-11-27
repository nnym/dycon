package net.auoeke.dycon;

import java.lang.invoke.ConstantBootstraps;
import java.util.function.Supplier;

public class Dycon {
	/**
	 The Dycon compiler plugin will replace each call to this method by an {@code ldc} instruction
	 that loads the dynamic constant result of invoking {@code initializer} once.
	 <p>
	 First it will extract a method handle from {@code initializer};
	 then it will replace the call to this method by loading a dynamic constant produced by applying
	 {@link ConstantBootstraps#invoke} to the handle as {@code handle}.
	 The lambda will be removed.
	 <p>
	 As an example, in the method
	 <pre>
Object lazyInvoke(Object... arguments) {
	// The expensive method handle calculation will occur only once.
	return ldc(() -> getMyMethodHandleSlowly()).invokeWithArguments(arguments);
}</pre>
	 the extracted handle will be called only when {@code lazyInvoke} is first called.
	 In subsequent calls to {@code lazyInvoke}, the result of the first call to
	 the extracted handle will be loaded instead.

	 @param initializer a supplier of the constant value whereby to replace the call to this method
	 @param <T> the type of the dynamic constant
	 @throws RuntimeException if invoked at runtime (for example reflectively or without the compiler plugin)
	 */
	public static <T> T ldc(Supplier<T> initializer) {
		throw new RuntimeException("Dycon::ldc may not be invoked at runtime");
	}
}
