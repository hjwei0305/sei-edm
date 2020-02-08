<!DOCTYPE html>

<html lang="en">
<head>
    <title>pageOffice</title>
</head>

<body>
<div style="width:1000px;height:800px;">${pageOffice} </div>
<script type="text/javascript">

    function Save() {
        document.getElementById("PageOfficeCtrl1").WebSave();
    }

    function AddSeal() {
        try {
            document.getElementById("PageOfficeCtrl1").ZoomSeal.AddSeal();
        } catch (e) {
        }
        ;
    }

    function PrintFile() {
        document.getElementById("PageOfficeCtrl1").ShowDialog(4);

    }

    function IsFullScreen() {
        document.getElementById("PageOfficeCtrl1").FullScreen = !document.getElementById("PageOfficeCtrl1").FullScreen;

    }

    function CloseFile() {
        window.external.close();
    }

    /**
     * @return {boolean}
     */
    function BeforeBrowserClosed() {
        if (document.getElementById("PageOfficeCtrl1").IsDirty) {
            if (confirm("提示：文档已被修改，是否继续关闭放弃保存 ？")) {
                return true;
            } else {
                return false;
            }
        }
    }

    //开启强留痕
    function SaveUpdateFile() {
        //关闭当前文件,打开另一个文件
        document.getElementById("PageOfficeCtrl1").Close();
        document.getElementById("PageOfficeCtrl1").WebOpen(filePath, 'docRevisionOnly', adminName);
    }

</script>
</body>
</html>