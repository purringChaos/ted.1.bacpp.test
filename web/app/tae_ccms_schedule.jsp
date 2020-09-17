<%@ page import="java.util.*, java.text.*, tv.blackarrow.cpp.model.*" language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<script type="text/javascript">
	function isRightFormat(str) {
		var patt1 = new RegExp("[0-9ABCabc][0-9]{7}[\.][sch]");
		return patt1.test(str) 
	}

	function resetForm() {
		var frm = document.schedule_form;
		frm.reset();
	}

	function validate() {
		var frm = document.schedule_form;
		var hasErrors = false;
		var message = "Please correct the following errors and try again";

		if(!isRightFormat(frm.filename.value)) {
			hasErrors = true;
			message = "\n* Please check CCMS name format\n"
		}		
		if(frm.schedule.value.length == 0) {
			hasErrors = true;
			message = message + "\n* Please enter CCMS schedule content"
		}

		if(hasErrors) {
			alert(message);
			return false;
		} else {
			var execFrm = document.schedule_form;
			execFrm.submit();
		}
	}
</script>

		<p><strong> Upload CCMS Schedule</strong></p>

<form name="schedule_form" action="<%= HostSettingBean.getInstance().getPoisHost() %>:6650/cpp/ccms" 
method="post"  onsubmit="return validate()">
	<table cellpadding="3" cellspacing="1" border="0">
		<tr>
			<td width="200" align="right">CCMS Name</td>
			<td width="10"></td>
			<td><input type="text" name="filename" size="20" value=""> (Example mddnnhhh.sch) &nbsp;&nbsp; m = month, nn = network 3, hhh = zone #</td>
		</tr><tr>
			<td valign="top" align="right">Schedule Content</td>
			<td></td>
			<td><textarea name="content" rows="20" cols="120" autofocus="autofocus"></textarea></td>
		</tr><tr>
			<td></td>
			<td></td>
			<td>
				<input type="submit" value="Submit" >&nbsp;&nbsp;
				<input type="reset" value="Reset">
			</td>
		</tr>
	</table>
</form>
