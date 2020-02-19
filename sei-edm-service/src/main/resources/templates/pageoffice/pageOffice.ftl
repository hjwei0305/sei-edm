<!DOCTYPE html>

<html lang="en">
<head>
    <title>附件管理(PageOffice)</title>
    <link rel="stylesheet" href="${baseUrl}/css/viewer.min.css">
    <link rel="stylesheet" href="${baseUrl}/css/loading.css">
    <link rel="stylesheet" href="${baseUrl}/css/bootstrap/bootstrap.min.css">
    <link rel="stylesheet" href="${baseUrl}/css/bootstrap/bootstrap-table.css"/>
    <script src="${baseUrl}/jquery.min.js" type="text/javascript"></script>
    <script src="${baseUrl}/pageoffice.js" id="po_js_main" type="text/javascript"></script>
</head>

<body>
<h1>SEI-EDM接入和测试界面(PageOffice)</h1>
<div class="panel-group" id="accordion">
<#--<div class="panel panel-default">-->
<#--<div class="panel-heading">-->
<#--<h4 class="panel-title">-->
<#--<a data-toggle="collapse" data-parent="#accordion"-->
<#--href="#collapseOne">-->
<#--接入说明-->
<#--</a>-->
<#--</h4>-->
<#--</div>-->
<#--<div id="collapseOne" class="panel-collapse collapse">-->
<#--<div class="panel-body">-->
<#--<div>-->
<#--如果你的项目需要接入文件预览项目，达到对docx、excel、ppt、jpg等文件的预览效果，那么通过在你的项目中加入下面的代码就可以-->
<#--成功实现：-->
<#--<pre style="background-color: #2f332a;color: #cccccc">-->
<#--$scope.openWin = function (fileUrl) {-->
<#--var url = configuration.previewUrl + encodeURIComponent(fileUrl);-->
<#--var winHeight = window.document.documentElement.clientHeight-10;-->
<#--$window.open(url, "_blank", "height=" + winHeight-->
<#--+ ",top=80,left=80,toolbar=no, menubar=no, scrollbars=yes, resizable=yes");-->
<#--};-->
<#--</pre>-->
<#--</div>-->
<#--<div>-->
<#--新增多图片同时预览功能，接口如下：-->
<#--<pre style="background-color: #2f332a;color: #cccccc">-->
<#--var fileUrl =url1+"|"+"url2";//多文件使用“|”字符隔开-->
<#--var url = "http://localhost:8012/picturesPreview?urls" + encodeURIComponent(fileUrl);-->
<#--var winHeight = window.document.documentElement.clientHeight-10;-->
<#--$window.open(url, "_blank", "height=" + winHeight-->
<#--+ ",top=80,left=80,toolbar=no, menubar=no, scrollbars=yes, resizable=yes");-->
<#--</pre>-->
<#--</div>-->
<#--</div>-->
<#--</div>-->
<#--</div>-->
    <div class="panel panel-default">
        <div class="panel-heading">
            <h4 class="panel-title">
                <a data-toggle="collapse" data-parent="#accordion"
                   href="#collapse3">
                    预览测试(MongoDB)
                </a>
            </h4>
        </div>
        <div id="collapse3" class="panel-collapse collapse in">
            <div class="panel-body">
                <p style="color: red;">
                    地址示例：</br>
                    上传：/upload POST 参数 <strong>file</strong> ${baseUrl}upload </br>
                    获取业务实体的文档信息清单：/getEntityDocumentInfos POST/GET 参数 <strong>entityId</strong> ${baseUrl}
                    getEntityDocumentInfos?entityId=XXX</br>
                    下载：/download GET 参数 <strong>docId</strong> ${baseUrl}download?docId=XXX</br>
                    预览：/pageOffice/preview GET  ${baseUrl}pageOffice/preview/{docId}</br>

                </p>
                <div style="padding: 10px">
                    <form enctype="multipart/form-data" id="fileUpload">
                        <input type="file" name="file"/>
                        <input type="button" id="btnsubmit" value=" 上 传 "/>
                    </form>
                <#--<form id="clearCache">-->
                <#--<input type="button" id="btnreset" value=" 清 空 " />-->
                <#--</form>-->
                </div>
                <div id="fileData">

                </div>
            </div>
        </div>
    </div>
</div>

<div class="loading_container">
    <div class="spinner">
        <div class="spinner-container container1">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
        <div class="spinner-container container2">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
        <div class="spinner-container container3">
            <div class="circle1"></div>
            <div class="circle2"></div>
            <div class="circle3"></div>
            <div class="circle4"></div>
        </div>
    </div>
</div>
<script src="${baseUrl}/js/jquery-3.0.0.min.js" type="text/javascript"></script>
<script src="${baseUrl}/js/jquery.form.min.js" type="text/javascript"></script>
<script src="${baseUrl}/js/bootstrap.min.js"></script>
<script src="${baseUrl}/js/bootstrap-table.js"></script>
<script>
    function deleteFile(fileName) {
        $.ajax({
            url: '${baseUrl}deleteFile?fileName=' + encodeURIComponent(fileName),
            success: function (data) {
                // 删除完成，刷新table
                $('#tableFS').bootstrapTable('refresh', {});
            },
            error: function (data) {
                console.log(data);
            }
        })
    }

    $(function () {
        $('#tableFS').bootstrapTable({
            url: 'listFiles',
            columns: [{
                field: 'fileName',
                title: '文件名'
            }, {
                field: 'action',
                title: '操作'
            }]
        }).on('pre-body.bs.table', function (e, data) {
            // 每个data添加一列用来操作
            $(data).each(function (index, item) {
                item.action = "<a class='btn btn-default' target='_blank' href='${baseUrl}onlinePreview?url="
                        + encodeURIComponent('${baseUrl}' + item.fileName) + "'>预览</a>" +
                        "<a class='btn btn-default' target='_blank' href='javascript:void(0);' onclick='deleteFile(\"" + item.fileName + "\")'>删除</a>";
            });
            return data;
        }).on('post-body.bs.table', function (e, data) {
            return data;
        });

        /**
         *
         */
        function showLoadingDiv() {
            var height = window.document.documentElement.clientHeight - 1;
            $(".loading_container").css("height", height).show();
        }

        $("#btnsubmitFS").click(function () {
            showLoadingDiv();
            $("#fileUploadFS").ajaxSubmit({
                success: function (data) {
                    // 上传完成，刷新table
                    if (!data.success) {
                        alert(data.message);
                    } else {
                        $('#tableFS').bootstrapTable('refresh', {});
                    }
                    $(".loading_container").hide();
                },
                error: function (error) {
                    alert(error);
                    $(".loading_container").hide();
                },
                url: 'fileUpload', /*设置post提交到的页面*/
                type: "post", /*设置表单以post方法提交*/
                dataType: "json" /*设置返回值类型为文本*/
            });
        });

        $("#btnresetFS").click(function () {
            showLoadingDiv();
            $("#clearCache").ajaxSubmit({
                success: function (data) {
                    // 上传完成，刷新table
                    if (!data.success) {
                        alert(data.message);
                    } else {
                        $('#tableFS').bootstrapTable('refresh', {});
                    }
                    $(".loading_container").hide();
                },
                error: function (error) {
                    alert(error);
                    $(".loading_container").hide();
                },
                url: 'clearCache', /*设置post提交到的页面*/
                type: "post", /*设置表单以post方法提交*/
                dataType: "json" /*设置返回值类型为文本*/
            });
        });


        //----------------------MongoDB---------------------
        $("#btnsubmit").click(function () {
            showLoadingDiv();
            $("#fileUpload").ajaxSubmit({
                success: function (data) {
                    // 上传完成，刷新table
                    console.log(JSON.stringify(data));
                    var html = '';

                    html =  "<a href=\"javascript:POBrowser.openWindowModeless('${baseUrl}pageOffice/preview/"+ data[0]['id']+"','width=1200px;height=900px;');\">预览" + data[0]['fileName'] + "</a></br>";
                    //html = "<a href='${baseUrl}pageOffice/preview/" + data[0]['id'] + "'>预览" + data[0]['fileName'] + "</a></br>";
                    html += "<a target='_blank' href='${baseUrl}download?docId=" + data[0]['id'] + "'>下载" + data[0]['fileName'] + "</a>";
                    $('#fileData').html(html);

                    $(".loading_container").hide();
                },
                error: function (error) {
                    alert(error);
                    $(".loading_container").hide();
                },
                url: '${baseUrl}/upload', /*设置post提交到的页面*/
                type: "post", /*设置表单以post方法提交*/
                dataType: "json" /*设置返回值类型为文本*/
            });
        });
    });
</script>
</body>
</html>