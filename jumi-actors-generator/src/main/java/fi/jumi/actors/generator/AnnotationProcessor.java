// Copyright © 2011-2015, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.actors.generator;

import com.google.common.base.Throwables;
import fi.jumi.actors.generator.ast.*;
import fi.jumi.actors.generator.codegen.GeneratedClass;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.tools.JavaFileObject;
import java.io.*;
import java.util.*;

import static javax.tools.Diagnostic.Kind.*;

@SupportedAnnotationTypes("fi.jumi.actors.generator.GenerateEventizer")
public class AnnotationProcessor extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        for (Element element : roundEnv.getElementsAnnotatedWith(GenerateEventizer.class)) {
            if (element.getKind() == ElementKind.INTERFACE) {
                log().printMessage(NOTE, "Generating eventizers for " + element, element);
                try {
                    generateEventizers((TypeElement) element);
                } catch (Exception e) {
                    log().printMessage(ERROR, "Failed to generate eventizers for " + element +
                            "\n" + Throwables.getStackTraceAsString(e), element);
                }
            } else {
                log().printMessage(WARNING, "Only interfaces can be annotated with @GenerateEventizer", element);
            }
        }
        return false;
    }

    private void generateEventizers(TypeElement eventInterface) throws IOException {
        GenerateEventizer config = eventInterface.getAnnotation(GenerateEventizer.class);

        String targetPackage;
        if (!config.targetPackage().isEmpty()) {
            targetPackage = config.targetPackage();
        } else {
            PackageElement currentPackage = getPackage(eventInterface);
            targetPackage = currentPackage.getQualifiedName().toString();
        }

        if (config.useParentInterface()) {
            List<? extends TypeMirror> parentInterfaces = eventInterface.getInterfaces();
            if (parentInterfaces.size() != 1) {
                log().printMessage(ERROR, "Expected one parent interface, but had " + parentInterfaces.size(), eventInterface);
                return;
            }
            // TODO: does not support interfaces that are members of an enclosing class (see EventStubGeneratorTest.getJavaSource)
            String parentInterface = parentInterfaces.get(0).toString();
            String sourceCode = new LibrarySourceLocator().findSources(parentInterface);
            if (sourceCode == null) {
                log().printMessage(ERROR, "Could not find source code for " + parentInterface, eventInterface);
                return;
            }
            eventInterface = AstExtractor.getAst(parentInterface, new JavaSourceFromString(parentInterface, sourceCode));
        }

        EventStubGenerator generator = new EventStubGenerator(eventInterface, new TargetPackageResolver(targetPackage));
        generator.setGeneratorName(getClass().getName());

        for (GeneratedClass generated : generator.getGeneratedClasses()) {
            JavaFileObject file = processingEnv.getFiler().createSourceFile(generated.name, eventInterface);
            Writer w = file.openWriter();
            w.write(generated.source);
            w.close();
        }
    }

    private static PackageElement getPackage(Element e) {
        while (e.getKind() != ElementKind.PACKAGE) {
            e = e.getEnclosingElement();
        }
        return (PackageElement) e;
    }


    @Override
    public SourceVersion getSupportedSourceVersion() {
        // avoid warnings on newer JVMs, even though we compile on JDK 6
        return processingEnv.getSourceVersion();
    }

    private Messager log() {
        return processingEnv.getMessager();
    }
}
