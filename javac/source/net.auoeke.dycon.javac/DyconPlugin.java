package net.auoeke.dycon.javac;

import javax.annotation.processing.Messager;
import javax.lang.model.element.Name;
import javax.lang.model.util.Elements;
import com.sun.source.util.JavacTask;
import com.sun.source.util.Plugin;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
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
import com.sun.tools.javac.util.Names;
import net.auoeke.reflect.Modules;

public class DyconPlugin implements Plugin, TaskListener {
	private Context context;
	private Elements elements;
	private Trees trees;
	private Symtab symtab;
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
		this.elements = task.getElements();
		this.trees = JavacTrees.instance(task);
		this.symtab = Symtab.instance(this.context);
		this.messager = JavacProcessingEnvironment.instance(this.context).getMessager();
		var names = Names.instance(this.context);

		this.invokeHandle = new Symbol.MethodHandleSymbol(this.symtab.enterClass(this.symtab.java_base, names.fromString("java.lang.invoke.ConstantBootstraps")).members().findFirst(
			names.fromString("invoke"),
			symbol -> symbol instanceof Symbol.MethodSymbol invoke
				&& invoke.params.size() == 5
				&& invoke.params.get(3).type.tsym == this.symtab.methodHandleType.tsym
		));

		this.net_auoeke_dycon = this.elements.getName("net.auoeke.dycon");
		this.Dycon = this.elements.getName("Dycon");
		this.ldc = this.elements.getName("ldc");

		task.addTaskListener(this);
	}

	@Override public boolean autoStart() {
		return true;
	}

	@Override public void started(TaskEvent event) {
		if (event.getKind() == TaskEvent.Kind.GENERATE) {
			var trees = (JavacTrees) this.trees;
			var cu = (JCTree.JCCompilationUnit) event.getCompilationUnit();
			var factory = TreeMaker.instance(this.context);

			new TreeTranslator() {
				@Override public void visitApply(JCTree.JCMethodInvocation node) {
					if (node.getMethodSelect() instanceof JCTree.JCFieldAccess field
						&& field.sym instanceof Symbol.MethodSymbol method
						&& method.owner.packge().fullname.equals(DyconPlugin.this.net_auoeke_dycon)
						&& method.owner.name.equals(DyconPlugin.this.Dycon)
						&& method.name.equals(DyconPlugin.this.ldc)
					) {
						var initializer = (Symbol.MethodHandleSymbol) ((Symbol.DynamicMethodSymbol) ((JCTree.JCFieldAccess) ((JCTree.JCMethodInvocation) node.getArguments().get(0)).getMethodSelect()).sym).staticArgs[1];
						var type = (Type.MethodType) initializer.type;
						this.result = factory.Ident(new Symbol.DynamicVarSymbol(
							(com.sun.tools.javac.util.Name) DyconPlugin.this.ldc,
							null,
							DyconPlugin.this.invokeHandle,
							type.getReturnType(),
							new PoolConstant.LoadableConstant[]{initializer}
						));
					} else {
						this.result = node;
					}
				}
			}.visitTopLevel(cu);
		}
	}

	@Override public void finished(TaskEvent event) {
		if (event.getKind() == TaskEvent.Kind.ANALYZE) {
		}
	}

	static {
		Modules.open(Plugin.class.getModule());
	}
}
