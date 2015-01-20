package kuvaldis.play.jackrabbit;

import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.jackrabbit.core.RepositoryImpl;
import org.apache.jackrabbit.core.config.RepositoryConfig;
import org.apache.jackrabbit.value.BinaryImpl;

import javax.jcr.*;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.function.IntUnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * @link http://jackrabbit.apache.org/oak/
 */
public class Main {
    public static void main(String[] args) throws RepositoryException, IOException {
        // repo creation
        final String xml = "/home/kuvaldis/Work/Mine/Study/jackrabbit/repo/configuration.xml";
        final String dir = "/home/kuvaldis/Work/Mine/Study/jackrabbit/repo/directory";
        final RepositoryConfig config = RepositoryConfig.create(xml, dir);
        final Repository repository = RepositoryImpl.create(config);

        // login
        final Session session = repository.login(new SimpleCredentials("admin", "admin".toCharArray()));

        // some actions
        try {
            checkParam(repository, session);
            workWithNodes(session);
            importXml(session);
            saveFile(session);
            workspaces(session);
        } finally {
            session.logout();
        }
    }

    private static void workspaces(Session session) throws RepositoryException {
        System.out.println("current workspace name = " + session.getWorkspace().getName());
        if (!Arrays.asList(session.getWorkspace().getAccessibleWorkspaceNames()).contains("another")) {
            session.getWorkspace().createWorkspace("another");
        }
    }

    private static void saveFile(Session session) throws RepositoryException, IOException {
        final Node root = session.getRootNode();
        final Node fileNode = root.addNode("file");
        final Binary fileBinary = new BinaryImpl(testFileStream());
        System.out.println("size before save = " + fileBinary.getSize());
        fileNode.setProperty("content", fileBinary);
        fileNode.setProperty("some small enough", getBinary(98));
        fileNode.setProperty("some not so small", getBinary(99));
        session.save();
        System.out.println("size after save = " + root.getNode("file").getProperty("content").getBinary().getSize());
    }

    private static BinaryImpl getBinary(final int size) {
        return new BinaryImpl(
                 IntStream.iterate(0, IntUnaryOperator.identity())
                         .limit(size)
                         .mapToObj(String::valueOf)
                         .collect(Collectors.joining()).getBytes());
    }

    private static void importXml(Session session) throws RepositoryException, IOException {
        Node root = session.getRootNode();
        if (!root.hasNode("importxml")) {
            System.out.print("Importing xml... ");
            Node node = root.addNode("importxml", "nt:unstructured");
            InputStream xml = testFileStream();
            session.importXML(node.getPath(), xml, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            xml.close();
            session.save();
            System.out.println("done.");
        }
        dump(root);
    }

    private static InputStream testFileStream() {
        return Main.class.getClassLoader().getResourceAsStream("test.xml");
    }

    private static void dump(Node node) throws RepositoryException {
        System.out.println(node.getPath());
        if (node.getName().equals("jcr:system")) {
            return;
        }

        PropertyIterator properties = node.getProperties();
        while (properties.hasNext()) {
            Property property = properties.nextProperty();
            if (property.getDefinition().isMultiple()) {
                Value[] values = property.getValues();
                for (int i = 0; i < values.length; i++) {
                    System.out.println(property.getPath() + " = " + values[i].getString());
                }
            } else {
                System.out.println(property.getPath() + " = " + property.getString());
            }
        }
        NodeIterator nodes = node.getNodes();
        while (nodes.hasNext()) {
            dump(nodes.nextNode());
        }
    }

    private static void workWithNodes(Session session) throws RepositoryException {
        final Node root = session.getRootNode();
        final Node hello = root.addNode("hello");
        final Node world = hello.addNode("world");
        world.setProperty("message", "Hello, world!");
        session.save();
        final Node helloWorld = root.getNode("hello/world");
        System.out.println("path = " + helloWorld.getPath());
        System.out.println("message = " + helloWorld.getProperty("message").getString());
        root.getNode("hello").remove();
        session.save();
        System.out.println("hello/world removed = " + (JcrUtils.getNodeIfExists("/hello/world", session) == null));
    }

    private static void checkParam(Repository repository, Session session) {
        final String user = session.getUserID();
        final String name = repository.getDescriptor(Repository.REP_NAME_DESC);
        System.out.println("user = " + user);
        System.out.println("name = " + name);
    }
}
