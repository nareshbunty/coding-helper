package com.antheminc.nimbus.ltss.admin.extension.handler;

import java.time.ZonedDateTime;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;

import com.anthemic.nimbus.ltss.admin.extension.employee.core.Employee;
import com.antheminc.adminbuilder.domains.core.utils.ParamUtils;
import com.antheminc.oss.nimbus.context.BeanResolverStrategy;
import com.antheminc.oss.nimbus.domain.cmd.Command;
import com.antheminc.oss.nimbus.domain.cmd.CommandBuilder;
import com.antheminc.oss.nimbus.domain.cmd.CommandMessage;
import com.antheminc.oss.nimbus.domain.cmd.exec.AbstractFunctionHandler;
import com.antheminc.oss.nimbus.domain.cmd.exec.CommandExecutorGateway;
import com.antheminc.oss.nimbus.domain.cmd.exec.CommandPathVariableResolver;
import com.antheminc.oss.nimbus.domain.cmd.exec.ExecutionContext;
import com.antheminc.oss.nimbus.domain.model.state.EntityState.Param;
import com.antheminc.oss.nimbus.support.JustLogit;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author AF75861 Naresh Bisa
 *
 * 
 */

public class EmployeeOperationsHandler<T, R>  extends AbstractFunctionHandler<T, R> {
	protected JustLogit logit = new JustLogit(this.getClass());

	@Autowired
	protected ObjectMapper objectMapper;
	
	@Autowired
	protected MongoTemplate mongoTemplate;

	CommandExecutorGateway executorGateway;
	CommandPathVariableResolver pathVariableResolver;
	
	public EmployeeOperationsHandler(BeanResolverStrategy beanResolver) {
		this.executorGateway = beanResolver.find(CommandExecutorGateway.class);
		this.pathVariableResolver = beanResolver.find(CommandPathVariableResolver.class);
	}
	
	@Override
	public R execute(ExecutionContext eCtx, Param<T> actionParameter) {
		Map<String, String[]> paramMap = eCtx.getCommandMessage().getCommand().getRequestParams();
		String source = null;
		String target = null;
		if (paramMap.get("source") != null) 
			source = paramMap.get("source")[0];
			
		if (paramMap.get("target") != null) 
			target = paramMap.get("target")[0];
		
		if (source != null) {
			switch (source) {
			case "addEmployee":
				addEmployee(actionParameter, target);
				break;
			}
		}		
		return null;
	}

	private void addEmployee(Param<T> actionParameter, String target) {
		Param<?> rootTaskParam = actionParameter.getRootDomain().getAssociatedParam();
		String editEmployeeForm = target;
		String formPath = "/";
		String firstName = ParamUtils.getStateValue(rootTaskParam, editEmployeeForm, "firstName");// it is example for  using target in function H'lr
		String lastName = ParamUtils.getStateValue(rootTaskParam, editEmployeeForm, "lastName");
		Integer age = (Integer)actionParameter.findParamByPath("/age").getState();// it is example for without using util
		String gender = ParamUtils.getStateValue(actionParameter, formPath, "gender");// it is example for without using target
		Double salary = ParamUtils.getStateValueAsDouble(rootTaskParam, editEmployeeForm, "salary");
		ZonedDateTime dob=ParamUtils.getStateValueAsZonedDate(rootTaskParam, editEmployeeForm, "dob");
		Boolean isExperienced = ParamUtils.getStateValueAsBoolean(actionParameter, formPath, "isExperienced");
		Employee emp = new Employee();
		emp.setAge(age);
		emp.setFirstName(firstName);
		emp.setLastName(lastName);
		emp.setDob(dob);
		emp.setSalary(salary);
		emp.setGender(gender);
		emp.setIsExperienced(isExperienced);
		String url= new String("/p/employees/_new");
		configExecute( actionParameter,  url,  emp);
	}

	private void configExecute(Param<T> actionParameter, String url, Object requestObj) {

		String resolveUrl = resolveCommandUrl(actionParameter, url);
		Command command = CommandBuilder.withUri(resolveUrl).getCommand();

		try {
			CommandMessage commandMessage = new CommandMessage(command, objectMapper.writeValueAsString(requestObj));
			executorGateway.execute(commandMessage);
		} catch (Exception e) {
			logit.error(() -> "Logging request object of " + requestObj.getClass().getName() + " is failed ...", e);
		}
	}
	
	private String resolveCommandUrl(Param<T> actionParameter, String commandUrl) {
		commandUrl = pathVariableResolver.resolve(actionParameter, commandUrl);
		commandUrl = actionParameter.getRootExecution().getRootCommand().getRelativeUri(commandUrl);
		return commandUrl;
	}
}
