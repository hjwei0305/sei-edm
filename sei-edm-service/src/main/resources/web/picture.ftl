<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>图片预览图</title>
    <meta http-equiv="x-ua-compatible" content="ie=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">

    <link rel="stylesheet" href="${baseUrl}css/viewer.min.css">
</head>
<body>
<img id="aImg" style="display:none;" src="${currentUrl}"  alt=""/>
<script src="${baseUrl}js/viewer.min.js"></script>
<script>
    var viewer = new Viewer(document.getElementById('aImg'), {
        navbar: false,
        button: false,
        backdrop: false
        // hidden: function () {
        //     viewer.destroy();
        // }

    });

    // image.click();
    viewer.show();
    document.getElementById('aImg').style = "display:none;";
</script>
</body>

</html>
