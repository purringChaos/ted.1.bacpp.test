		<p><strong> Manifest Confirmation Condition Event Post</strong></p>
		Please fill in ManifestConfirmationConditionEvent XML format data<br /><br /> 
		<form name="uploadPoForm" action="<%= HostSettingBean.getInstance().getTranscoderHost() %>:6800/transcode/manifest" method="post" enctype="text/plain">
			<textarea name="signal" rows="20" cols="120" autofocus="autofocus"></textarea>
			<br /><br />
			<input type="submit" value="Submit" />
			<input type="reset" value="Reset" />
		</form>
