package com.yu.butterknife_compiler;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import com.yu.butterknife_annotation.BindView;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

/**
 * @author luyanjun
 * @description  generate java file
 */

//1. 必须添加AutoService注解
@AutoService(Processor.class)
public class ButterKnifeProcesser extends AbstractProcessor {
    private Filer mFiler;
    private Elements mElementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mFiler = processingEnvironment.getFiler();
        mElementUtils = processingEnvironment.getElementUtils();
    }

    // 2.  设置支持最新的版本
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latestSupported();
    }

    // 3. 获取到需要被处理的注解
    @Override
    public Set<String> getSupportedAnnotationTypes() {
        Set<String> types = new LinkedHashSet<>();
        for (Class<? extends Annotation> annotation : getSupportedAnnotations()) {
            types.add(annotation.getCanonicalName());
        }
        return types;
    }

    private Set<Class<? extends Annotation>> getSupportedAnnotations() {
        Set<Class<? extends Annotation>> annotations = new LinkedHashSet<>();
        // 把需要被处理的注解添加到set中
        annotations.add(BindView.class);
        return annotations;
    }

    //4. 自动生成代码 process 方法代表的是，有注解就都会进来 ，但是这里面是一团乱麻
    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {

        //4.1 获取并处理元素
        // 4.1.1 获取到所有有BindView注解的元素
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(BindView.class);
        // 4.1.2 创建 Map ,key 为 Activity 的元素，value为其中有BindView注解的元素
        Map<Element, List<Element>> elementsMap = new LinkedHashMap<>();
        for (Element element : elements) {
            // 获取到每个element 对应的Activity的元素
            Element enclosingElement = element.getEnclosingElement();
            List<Element> viewBindElements = elementsMap.get(enclosingElement);
            if (viewBindElements == null) {
                viewBindElements = new ArrayList<>();
                elementsMap.put(enclosingElement, viewBindElements);
            }
            // 将element存放到对应的value中
            viewBindElements.add(element);
        }

        // 4.2 遍历map 每个Activity生成一个对应的java文件
        for (Map.Entry<Element, List<Element>> entry : elementsMap.entrySet()) {
            Element enclosingElement = entry.getKey();
            List<Element> viewBindElements = entry.getValue();

            // 4.2.1 创建类 public final class xxxActivity_ViewBinding implements Unbinder
            String activityClassNameStr = enclosingElement.getSimpleName().toString();
            //获取类名
            ClassName activityClassName = ClassName.bestGuess(activityClassNameStr);
            //获取实现的接口名
            ClassName unbinderClassName = ClassName.get("com.yu.knife","Unbinder");
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(activityClassNameStr+"_ViewBinding")
                    //添加修饰符
                    .addModifiers(Modifier.FINAL,Modifier.PUBLIC)
                    //实现接口
                    .addSuperinterface(unbinderClassName)
                    //添加字段
                    .addField(activityClassName,"target",Modifier.PRIVATE);


            // 4.2.2 实现 unbind 方法
            //androidx.annotation.CallSuper注解
            ClassName callSuperClassName = ClassName.get("androidx.annotation","CallSuper");
            MethodSpec.Builder unbindMethodBuilder = MethodSpec.methodBuilder("unbind")
                    //添加Override注解
                    .addAnnotation(Override.class)
                    .addModifiers(Modifier.PUBLIC,Modifier.FINAL)
                    .addAnnotation(callSuperClassName);
            //在方法中添加代码 $T 和 $L 都是占位符
            unbindMethodBuilder.addStatement("$T target = this.target",activityClassName);
            unbindMethodBuilder.addStatement("if (target == null) throw new IllegalStateException(\"Bindings already cleared.\");");

            // 4.2.3 构造函数
            MethodSpec.Builder constructorMethodBuilder = MethodSpec.constructorBuilder()
                    //添加参数  类型为对应的Activity
                    .addParameter(activityClassName,"target");
            // this.target = target;
            constructorMethodBuilder.addStatement("this.target = target");
            // findViewById 属性
            for (Element viewBindElement : viewBindElements) {
                // target.textView1 = Utils.findViewById(source, R.id.tv1);
                String filedName = viewBindElement.getSimpleName().toString();
                ClassName utilsClassName = ClassName.get("com.yu.knife","Utils");
                int resId = viewBindElement.getAnnotation(BindView.class).value();
                constructorMethodBuilder.addStatement("target.$L = $T.findViewById(target, $L)",filedName,utilsClassName,resId);
                // target.textView1 = null; 这个是在unBind()中添加的
                unbindMethodBuilder.addStatement("target.$L = null",filedName);
            }

            //4.2.4 把方法添加到类中
            classBuilder.addMethod(unbindMethodBuilder.build());
            classBuilder.addMethod(constructorMethodBuilder.build());

            // 4.2.5 生成类
            try {
                //获取包名
                String packageName = mElementUtils.getPackageOf(enclosingElement).getQualifiedName().toString();

                JavaFile.builder(packageName,classBuilder.build())
                        //文件注释
                        .addFileComment("butterknife 自动生成")
                        .build().writeTo(mFiler);
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println("翻车了！");
            }
        }
        return false;
    }
}
