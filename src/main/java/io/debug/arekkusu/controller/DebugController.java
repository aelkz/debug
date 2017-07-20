package io.debug.arekkusu.controller;

import io.debug.arekkusu.model.Debug;
import org.apache.catalina.connector.Connector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.embedded.EmbeddedWebApplicationContext;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainer;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;

import javax.management.*;
import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

// http://www.concretepage.com/spring-4/spring-4-rest-cors-integration-using-crossorigin-annotation-xml-filter-example
// https://github.com/spring-projects/spring-boot/issues/5834
// https://stackoverflow.com/questions/38916213/how-to-get-the-spring-boot-host-and-port-address-during-run-time
// https://stackoverflow.com/questions/14459210/getservletconfig-getservletcontext-equivalent-in-spring
// https://stackoverflow.com/questions/22872434/retrieve-the-port-of-the-server-on-startup-of-servlet-container-independent-of
// https://stackoverflow.com/questions/1490869/how-to-get-vm-arguments-from-inside-of-java-application
// http://www.xyzws.com/javafaq/how-to-get-my-current-system-properties-in-java/193
// https://stackoverflow.com/questions/40401383/spring-boot-get-application-base-url-outside-of-servlet-context
// https://stackoverflow.com/questions/37475810/spring-how-to-get-servletcontext-into-a-service-how-to-get-webapps-manifes

@RestController
//@RequestMapping("/debug") => dont need root context mapping (it is the {package}.war name by default)
@CrossOrigin(origins = {"http://localhost:18080", "http://localhost:28080"}, allowCredentials = "false")
public class DebugController {

    @Autowired
    Environment environment;

    @Autowired
    ServletContext servletContext;

    @Autowired
    HttpServletRequest httpServletRequest;

    @RequestMapping(method = RequestMethod.GET)
    public Debug getServerStatus(){
        Debug obj = new Debug();
        List<String> tempList = new ArrayList<>();

        // server information
        obj.setHost_app_server(servletContext.getServerInfo());
        obj.setHost_app_servlet_version(servletContext.getEffectiveMajorVersion() + "." + servletContext.getEffectiveMinorVersion());
        obj.setHost_virtual_server_name(servletContext.getVirtualServerName());
        obj.setContext_path(servletContext.getContextPath());
        obj.setRequest_url(httpServletRequest.getRequestURL().toString());
        obj.setRequest_uri(httpServletRequest.getRequestURI());
        obj.setProtocol(httpServletRequest.getProtocol());
        obj.setScheme(httpServletRequest.getScheme());
        obj.setLocal_port(String.valueOf(httpServletRequest.getLocalPort()));
        obj.setRequest_method(httpServletRequest.getMethod());

        try {
            obj.setRemote_port(""+httpServletRequest.getRemotePort());
        }catch (Exception e) {}

        // client information
        try {
            obj.setRemote_address(InetAddress.getLocalHost().getHostAddress());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        try {
            obj.setRemote_host(InetAddress.getLocalHost().getHostName());
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        obj.setServer_name(httpServletRequest.getServerName());
        obj.setServer_port(String.valueOf(httpServletRequest.getServerPort()));

        // server system properties
        Properties p = System.getProperties();
        Enumeration keys = p.keys();
        while (keys.hasMoreElements()) {
            String key = (String)keys.nextElement();
            String value = (String)p.get(key);
            System.out.println(key + ": " + value);
            tempList.add(key + ": " + value);
        }

        obj.setSystem_properties(tempList);

        // server vm arguments
        RuntimeMXBean runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        List<String> arguments = runtimeMxBean.getInputArguments();
        obj.setVm_arguments(arguments);

        // server ports information
        MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
        Set<ObjectName> objs = null;
        try {
            objs = mbs.queryNames(new ObjectName("*:type=Connector,*"),
                    Query.match(Query.attr("protocol"), Query.value("HTTP/1.1")));
            String hostname = InetAddress.getLocalHost().getHostName();
            InetAddress[] addresses = InetAddress.getAllByName(hostname);
            ArrayList<String> endPoints = new ArrayList<String>();
            for (Iterator<ObjectName> i = objs.iterator(); i.hasNext();) {
                ObjectName objx = i.next();
                String scheme = mbs.getAttribute(objx, "scheme").toString();
                String port = objx.getKeyProperty("port");
                for (InetAddress addr : addresses) {
                    String host = addr.getHostAddress();
                    String ep = scheme + "://" + host + ":" + port;
                    endPoints.add(ep);
                }
            }
            obj.setEndpoints(endPoints);
        } catch (MalformedObjectNameException e) {
            e.printStackTrace();
        } catch (ReflectionException e) {
            e.printStackTrace();
        } catch (InstanceNotFoundException e) {
            e.printStackTrace();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        } catch (AttributeNotFoundException e) {
            e.printStackTrace();
        } catch (MBeanException e) {
            e.printStackTrace();
        }

        return obj;
    }

    //@RequestMapping(method = RequestMethod.GET, value="/{routeId}/{userLocation}")
    //public Debug getRouteTrafficData(@PathVariable("routeId") String routeId, @MatrixVariable("userLocation") String location){
    //    return null;
    //}
}
