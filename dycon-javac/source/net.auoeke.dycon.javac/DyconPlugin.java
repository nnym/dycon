package net.auoeke.dycon.javac;

import javax.lang.model.element.Name;
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
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Names;
import net.auoeke.reflect.Modules;

public class DyconPlugin implements Plugin, TaskListener {
	private Context context;
	private Trees trees;

	private Symbol.MethodHandleSymbol invokeHandle;

	private Name net_auoeke_dycon;
	private Name Dycon;
	private Name ldc;

	@Override public String getName() {
		return "dycon";
	}

	@Override public void init(JavacTask task, String... args) {
		this.context = ((BasicJavacTask) task).getContext();
		this.trees = Trees.instance(task);
		var symtab = Symtab.instance(this.context);
		var names = Names.instance(this.context);

		this.invokeHandle = new Symbol.MethodHandleSymbol(symtab.enterClass(symtab.java_base, names.fromString("java.lang.invoke.ConstantBootstraps")).members().findFirst(
			names.fromString("invoke"),
			symbol -> symbol instanceof Symbol.MethodSymbol invoke
				&& invoke.params.size() == 5
				&& invoke.params.get(3).type.tsym == symtab.methodHandleType.tsym
		));

		this.net_auoeke_dycon = names.fromString("net.auoeke.dycon");
		this.Dycon = names.fromString("Dycon");
		this.ldc = names.fromString("ldc");

		task.addTaskListener(this);
	}

	@Override public boolean autoStart() {
		return true;
	}

	@Override public void started(TaskEvent event) {
		if (event.getKind() == TaskEvent.Kind.GENERATE) {
			var cu = (JCTree.JCCompilationUnit) event.getCompilationUnit();
			var factory = TreeMaker.instance(this.context);

			new TreeTranslator() {
				@Override public void visitApply(JCTree.JCMethodInvocation node) {
					super.visitApply(node);

					if (node.getMethodSelect() instanceof JCTree.JCFieldAccess field
						&& field.sym instanceof Symbol.MethodSymbol method
						&& method.owner.packge().fullname.equals(DyconPlugin.this.net_auoeke_dycon)
						&& method.owner.name.equals(DyconPlugin.this.Dycon)
						&& method.name.equals(DyconPlugin.this.ldc)
					) {
						var metafactory = (JCTree.JCMethodInvocation) node.getArguments().get(0);
						var initializer = (Symbol.MethodHandleSymbol) ((Symbol.DynamicMethodSymbol) ((JCTree.JCFieldAccess) metafactory.getMethodSelect()).sym).staticArgs[1];
						var type = initializer.type.asMethodType();

						if (type.getParameterTypes().size() > 0 || !initializer.isStatic()) {
							DyconPlugin.this.trees.printMessage(Diagnostic.Kind.ERROR, "capturing or non-static lambda cannot be intrinsified", node, cu);
						} else {
							this.result = factory.Ident(new Symbol.DynamicVarSymbol(
								(com.sun.tools.javac.util.Name) DyconPlugin.this.ldc,
								null,
								DyconPlugin.this.invokeHandle,
								type.getReturnType(),
								new PoolConstant.LoadableConstant[]{initializer}
							));
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
