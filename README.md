Dycon is a group of 2 projects:
- dycon: a single class `Dycon` containing a method that can be invoked to trigger dycon-javac
- dycon-javac: a plugin for the standard Java compiler that replaces calls to `Dycon::ldc` by `ldc` instructions.

Consider the example below.
```java
private static final T expensiveObject = expensiveInitialization();

public static U doStuff() {
	return expensiveObject.getU();
}
```
Since `expensiveObject` takes a while to initialize, is not always necessary and might be initialized as a side effect
of access to the implicit class for another reason, its initialization can waste much time. It might also cause class
loading circle. One can work around these problems by using the following patterns.
```java
private static T expensiveObject;

private static T expensiveObject() {
	if (expensiveObject == null) {
		synchronized (This.class) {
			if (expensiveObject == null) {
				expensiveObject = expensiveInitialization();
			}
		}
	}

	return expensiveObject;
}
```
```java
private static T expensiveObject() {
	class Holder {
		static final T expensiveObject = expensiveInitialization();
	}

	return Holder.expensiveObject;
}
```
The first is quite verbose and the second produces an extra class; which can become quite bad when used many times.

Dycon exploits the constant pool form `CONSTANT_Dynamic` introduced to Java 11 by
[JEP 309](https://openjdk.org/jeps/309) in order to provide a more elegant solution to these problems.
[JEP 303](https://openjdk.org/jeps/303) is a candidate that proposes intrinsics for `ldc` and `invokedynamic`. This
project provides a simpler interface for `ldc` intrinsics for arbitrary dynamic constants.
```java
import static net.auoeke.dycon.Dycon.ldc;
...
private static T expensiveObject() {
	return ldc(This::expensiveInitialization);
	// or return ldc(() -> expensiveInitialization());
}
```
dycon-javac finds this `ldc` invocation and extracts a handle to the target of the method reference or lambda and
replaces the call to `ldc` and the generation of the `Supplier` object by an `ldc` instruction with an index to a
`CONSTANT_Dynamic` constant pool entry that points to a bootstrap method that invokes the method handle. After the first
call `ldc` will reuse its result.

## download

Dycon is available from Central.
```groovy
dependencies {
	annotationProcessor("net.auoeke:dycon-javac:latest.release")
	compileOnly("net.auoeke:dycon:latest.release")
}
```

## miscellanea

```java
static int count;

static Unsafe lazyUnsafe() {
    return Dycon.ldc(() -> {
        ++count;
        return (Unsafe) MethodHandles.privateLookupIn(Unsafe.class, MethodHandles.lookup())
            .findStaticGetter(Unsafe.class, "theUnsafe", Unsafe.class)
            .invokeExact();
    });
}

@Test void test() {
    assert count == 0 : count;
    assert lazyUnsafe() != null;
    assert count == 1 : count;
    assert lazyUnsafe() == lazyUnsafe();
    assert count == 1 : count;
}
```

"Dycon" is derived from "condy" (common abbreviation of `CONSTANT_Dynamic`).

