<%@taglib prefix="f" uri="http://java.sun.com/jsf/core"%>
<%@taglib prefix="h" uri="http://java.sun.com/jsf/html"%>

<html>
<head><title>Hello World</title></head>
<body>
<f:view>
		<p>
			<h:message id="errors" for="firstName" style="color:red"/>
			<h:message id="errors1" for="lastName" style="color:red"/>
		</p>
	<h:form>
			<h:outputText value="First Name"></h:outputText>
			<h:inputText id="firstName" value="#{helloWorldBean.firstName}" 
			required="true">
			</h:inputText>
			<h:outputText value="Last Name"></h:outputText>
			<h:inputText id="lastName" value="#{helloWorldBean.lastName}" 
			required="true"></h:inputText>
			<h:commandButton action="#{helloWorldBean.sayHelloWorld}"
				value="Get Complete Name"></h:commandButton>
	</h:form>
</f:view>
</body>
</html>