// Copyright © 2011-2012, Esko Luontola <www.orfjackal.net>
// This software is released under the Apache License 2.0.
// The license text is at http://www.apache.org/licenses/LICENSE-2.0

package fi.jumi.core.files;

import fi.jumi.actors.ActorRef;

import javax.annotation.concurrent.NotThreadSafe;
import java.net.*;
import java.util.List;

@NotThreadSafe
public class FileSystemTestClassFinder implements TestClassFinder {

    private final List<URI> classPath;
    private final String includedTestsPattern;

    public FileSystemTestClassFinder(List<URI> classPath, String includedTestsPattern) {
        this.classPath = classPath;
        this.includedTestsPattern = includedTestsPattern;
    }

    @Override
    public void findTestClasses(ActorRef<TestClassFinderListener> listener) {
        try {
            // TODO: find all test classes from classpath
            // TODO: class loader might need to be dependency injected
            URLClassLoader loader = new URLClassLoader(asUrls(classPath));
            Class<?> testClass = loader.loadClass(includedTestsPattern);
            listener.tell().onTestClassFound(testClass);

        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        } catch (ClassNotFoundException e) {
            // no class matching the pattern; fail silently
        }
    }

    private static URL[] asUrls(List<URI> uris) throws MalformedURLException {
        URL[] urls = new URL[uris.size()];
        for (int i = 0, filesLength = uris.size(); i < filesLength; i++) {
            urls[i] = uris.get(i).toURL();
        }
        return urls;
    }
}
