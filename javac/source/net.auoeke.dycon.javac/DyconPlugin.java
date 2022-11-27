package net.auoeke.dycon.javac;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.api.BasicJavacTask;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symtab;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.jvm.PoolConstant;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.DiagnosticSource;
import com.sun.tools.javac.util.JavacMessages;
import com.sun.tools.javac.util.Log;
import com.sun.tools.javac.util.Names;
import net.auoeke.reflect.Modules;

public class DyconPlugin implements Plugin, TaskListener {
	private Context context;
	private Messager messager;

	private Symbol.MethodHandleSymbol invokeHandle;

	private Name net_auoeke_dycon;
	private Name Dycon;
	private Name ldc;

	@Override public String getName() {
		return "dycon";
	}

	@Override public void init(JavacTask task, String... args) {
		this.context = ((BasicJavacTask) task).getContext();
		this.messager = JavacProcessingEnvironment.instance(this.context).getMessager();
		var symtab = Symtab.instance(this.context);
		var names = Names.instance(this.context);
		JavacMessages.instance(this.context).add(l -> Resources.instance);

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
			var log = Log.instance(this.context);

			new TreeTranslator() {
				@Override public void visitApply(JCTree.JCMethodInvocation node) {
					this.result = node;

					if (node.getMethodSelect() instanceof JCTree.JCFieldAccess field
						&& field.sym instanceof Symbol.MethodSymbol method
						&& method.owner.packge().fullname.equals(DyconPlugin.this.net_auoeke_dycon)
						&& method.owner.name.equals(DyconPlugin.this.Dycon)
						&& method.name.equals(DyconPlugin.this.ldc)
					) {
						var metafactory = (JCTree.JCMethodInvocation) node.getArguments().get(0);
						var initializer = (Symbol.MethodHandleSymbol) ((Symbol.DynamicMethodSymbol) ((JCTree.JCFieldAccess) metafactory.getMethodSelect()).sym).staticArgs[1];
						var type = (Type.MethodType) initializer.type;

						if (type.getParameterTypes().size() > 0 || !initializer.isStatic()) {
							log.report(log.diags.error(null, new DiagnosticSource(cu.sourcefile, log), node.pos(), Resources.capturingOrStaticLambda()));
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
