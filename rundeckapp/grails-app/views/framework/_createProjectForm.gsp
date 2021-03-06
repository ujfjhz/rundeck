<%@ page contentType="text/html;charset=UTF-8" %>
<html>
<head>
    <g:set var="rkey" value="${g.rkey()}"/>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <meta name="layout" content="base"/>
    <meta name="tabpage" content="nodes"/>
    <title><g:message code="domain.Project.choose.title" default="Create a Project"/></title>

    <g:javascript library="prototype/effects"/>
    <g:javascript library="resourceModelConfig"/>
    <g:javascript>

    var configControl;
    function init(){
        configControl=new ResourceModelConfigControl('${prefixKey.encodeAsJavaScript()}');
        configControl.pageInit();
        $$('input').each(function(elem){
            if(elem.type=='text'){
                elem.observe('keypress',noenter);
            }
        });
    }
    Event.observe(window, 'load', init);
    </g:javascript>
    <style type="text/css">
    #configs li {
        margin-top: 5px;
    }

    div.buttons {
        text-align: right;
    }
    </style>
</head>

<body>
<g:set var="adminauth"
       value="${auth.resourceAllowedTest(type: 'resource', kind: 'project', action: ['create'], context: 'application')}"/>
<g:if test="${adminauth}">
    <div class="row">
    <div class="col-sm-12 col-md-10 col-md-offset-1 col-lg-8 col-lg-offset-2">
        <g:form action="createProject" method="post" onsubmit="return configControl.checkForm();">
            <div class="panel panel-primary"  id="createform">
                <div class="panel-heading">
                    <span class="h3">
                        <g:message code="domain.Project.create.message" default="Create a new Project"/>
                    </span>
                </div>
                <tmpl:editProjectForm/>
                <div class="panel-footer">
                    <g:submitButton name="create" value="${g.message(code: 'button.action.Create', default: 'Create')}" class="btn btn-default"/>
                </div>
            </div>
        </g:form>
    </div>
    </div>

</g:if>
<g:else>
    <div class="pageBody">
        <div class="error note"><g:message code="unauthorized.project.create"/></div>
    </div>
</g:else>
</body>
</html>
