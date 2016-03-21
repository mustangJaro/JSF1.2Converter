<%@ taglib uri="http://java.sun.com/jsf/core" prefix="f"%>
<%@ taglib uri="http://java.sun.com/jsf/html" prefix="h"%>
<%@ taglib uri="http://richfaces.org/rich" prefix="rich"%>
<%@ taglib uri="http://richfaces.org/a4j" prefix="a4j"%>

<html>
<head>
<title>Hello World</title>
</head>
<body>
	<f:view>
		<p>
			<h:message id="errors" for="firstName" style="color:red" />
			<h:message id="errors1" for="lastName" style="color:red" />
		</p>
		<h:form>
			<%@ include file="/mainMenu.jsp" %>
			<h:outputText value="First Name"></h:outputText>
			<h:inputText id="firstName" value="#{helloWorldBean.firstName}"
				required="true">
			</h:inputText>
			<h:outputText value="Last Name"></h:outputText>
			<h:inputText id="lastName" value="#{helloWorldBean.lastName}"
				required="true"></h:inputText>
			<h:commandButton action="#{helloWorldBean.sayHelloWorld}"
				value="Get Complete Name"></h:commandButton>
			<a4j:commandLink ajaxSingle="true" value="Show Comment"
				oncomplete="Richfaces.showModalPanel('mypanel');"
				id="commentOpenModalPanel" />
		</h:form>


		<rich:modalPanel id="mypanel" autosized="true">
			<f:facet name="header">
				<h:panelGroup>
					<h:outputText value="Comments"></h:outputText>
				</h:panelGroup>
			</f:facet>
			<f:facet name="controls">
				<a4j:commandLink id="exitbutton" value="Exit"
					onclick="Richfaces.hideModalPanel('mypanel')" ajaxSingle="true">
				</a4j:commandLink>
			</f:facet>
			<h:outputText value="modal panel test value" />
		</rich:modalPanel>


		<a4j:status onstart="#{rich:component('wait')}.show()"
			onstop="#{rich:component('wait')}.hide()" />
		<rich:modalPanel id="wait" autosized="true" width="200" height="60"
			moveable="false" resizeable="false">
			<f:facet name="header">
				<h:outputText value="Comments" />
			</f:facet>
			<h:outputText value="Loading Comment..."
				style="height:50px; width:60px; vertical-align:middle; text-align:center; text-decoration: blink;" />
		</rich:modalPanel>

	</f:view>


</body>
</html>