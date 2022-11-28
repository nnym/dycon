package net.auoeke.dycon.javac;

import java.util.HashMap;
import java.util.Map;
import javax.tools.Diagnostic;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.jvm.PoolConstant;
import com.sun.tools.javac.model.JavacElements;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Name;
import com.sun.tools.javac.util.Names;
import net.auoeke.reflect.Modules;

public class DyconPlugin implements Plugin, TaskListener {
	// Contains a unique name for each ldc call in a compilation unit in order to prevent deduplication.
	private final Map<Integer, Name> condyNames = new HashMap<>();
	private Context context;
	private JavacElements elements;
	private Trees trees;
	private TreeMaker factory;
	private Symtab symtab;
	private Names names;

	private Name net_auoeke_dycon;
	private Name Dycon;
	private Name ldc;

	private Symbol.MethodHandleSymbol invokeHandle;

	@Override public String getName() {
		return "dycon";
	}

	@Override public void init(JavacTask task, String... args) {
		this.context = ((BasicJavacTask) task).getContext();
		this.elements = JavacElements.instance(this.context);
		this.trees = Trees.instance(task);
		this.factory = TreeMaker.instance(this.context);
		this.symtab = Symtab.instance(this.context);
		this.names = Names.instance(this.context);
		this.net_auoeke_dycon = this.names.fromString("net.auoeke.dycon");
		this.Dycon = this.names.fromString("Dycon");
		this.ldc = this.names.fromString("ldc");

		task.addTaskListener(this);
	}

	@Override public boolean autoStart() {
		return true;
	}

	@Override public void started(TaskEvent event) {
		if (event.getKind() == TaskEvent.Kind.GENERATE) {
			var cu = (JCTree.JCCompilationUnit) event.getCompilationUnit();

			new TreeTranslator() {
				int index = 0;

				@Override public void visitApply(JCTree.JCMethodInvocation node) {
					super.visitApply(node);

					if (node.meth instanceof JCTree.JCFieldAccess field
						&& field.sym instanceof Symbol.MethodSymbol method
						&& method.owner.packge().fullname.equals(DyconPlugin.this.net_auoeke_dycon)
						&& method.owner.name.equals(DyconPlugin.this.Dycon)
						&& method.name.equals(DyconPlugin.this.ldc)
					) {
						if (node.args.head instanceof JCTree.JCMethodInvocation lmfCall && lmfCall.meth instanceof JCTree.JCFieldAccess lmf && lmf.sym instanceof Symbol.DynamicMethodSymbol bsm) {
							var initializer = (Symbol.MethodHandleSymbol) bsm.staticArgs[1];
							var type = initializer.type.asMethodType();

							if (type.getParameterTypes().size() > 0 || !initializer.isStatic()) {
								DyconPlugin.this.trees.printMessage(Diagnostic.Kind.ERROR, "capturing or non-static lambda cannot be intrinsified", node.args.head, cu);
							} else {
								if (DyconPlugin.this.invokeHandle == null) {
									DyconPlugin.this.invokeHandle = new Symbol.MethodHandleSymbol(DyconPlugin.this.elements.getTypeElement("java.lang.invoke.ConstantBootstraps").members().findFirst(
										DyconPlugin.this.names.fromString("invoke"),
										symbol -> symbol instanceof Symbol.MethodSymbol invoke
											&& invoke.params.size() == 5
											&& invoke.params.get(3).type.tsym == DyconPlugin.this.symtab.methodHandleType.tsym
									));
								}

								this.result = DyconPlugin.this.factory.Ident(new Symbol.DynamicVarSymbol(
									DyconPlugin.this.condyNames.computeIfAbsent(this.index++, index -> DyconPlugin.this.names.fromString(index.toString())),
									null,
									DyconPlugin.this.invokeHandle,
									type.getReturnType(),
									new PoolConstant.LoadableConstant[]{initializer}
								));
							}
						} else {
							DyconPlugin.this.trees.printMessage(Diagnostic.Kind.ERROR, "ldc argument is not a functional expression", node.args.head, cu);
						}
					}
				}
			}.visitTopLevel(cu);
		}
	}

	static {
		Modules.open(Plugin.class.getModule());
	}
}
