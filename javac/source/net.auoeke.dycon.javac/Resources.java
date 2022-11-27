package net.auoeke.dycon.javac;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;
import com.sun.tools.javac.util.JCDiagnostic;

public class Resources extends ResourceBundle {
	static final Resources instance = new Resources();

	private static final String capturingOrNonStaticLambda = error("capturing.or.non.static", "capturing or non-static lambda cannot be intrinsified");

	private final Map<String, String> entries = new HashMap<>();

	static JCDiagnostic.Error capturingOrStaticLambda() {
		return new JCDiagnostic.Error("dycon", capturingOrNonStaticLambda);
	}

	private static String error(String key, String value) {
		instance.entries.put("dycon.err." + key, value);
		return key;
	}

	@Override protected Object handleGetObject(String key) {
		return this.entries.get(key);
	}

	@Override public Enumeration<String> getKeys() {
		return Collections.enumeration(this.entries.keySet());
	}
}
