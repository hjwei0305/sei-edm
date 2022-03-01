## 测试环境访问方式
* 转换接口URL前缀：https://tccps.changhong.com/dcs-test/request
* 浏览接口URL前缀：https://tccps.changhong.com/dcs-test/preview
* 访问token：
    * 查询参数：X-Stargate-App-Token=f080999d16f84be5b956b8de90812d36
    * 若访问接口不带该查询参数，将返回http 401 授权失败错误

## 生产环境访问方式
* 转换接口URL前缀：https://ccps.changhong.com/dcs/request
* 浏览接口URL前缀：https://ccps.changhong.com/dcs/preview
* 访问token：
    请参照`合理使用转换后文档的访问链接`节申请后使用。

## 功能列表

### 1. 预览接口(html)
* 接口：POST /api/v1/preview/html?source_url=xxx&page_range=m,n
* 功能：支持将word,ppt,excel,pdf等类型文档转换为html，并返回预览链接
* 入参：
    1. source_url：待生成预览链接的url，此url中必须包含文件后缀名信息，目前可识别的文件后缀名有：.doc, .docx, .ppt, .pptx, .xls, .xlsx, .pdf
    2. page_range必须为`m,n`格式，m和n均为数值，m为起始页码，n为结束页码
* 出参：
    1. $.data.preview_url：预览链接。
    2. 注意：访问此预览链接时，需带X-Stargate-App-Token查询参数，通过api网关访问授权后方可访问到内容，否则返回http 401。
* 其它说明：
    1. 目前预览仅支持url形式输入被转换文档，暂不支持本地文件上传方式（业务需要时添加）。
    
### 2. 转换接口（pdf）
* 接口：POST /api/v1/convert/to_pdf?source_url=xxx
* 功能：支持将word,ppt,excel等文档转换为pdf文档，并返回访问链接
* 入参：
    1. source_url：待转换文档url，此url中必须包含文件后缀名信息，目前可识别的文件后缀名有：.doc, .docx, .ppt, .pptx, .xls, .xlsx
* 出参：
    1. $.data.preview_url：预览链接。
    2. $.data.pdf_url：转换后的pdf文档下载地址。
    3. 注意：访问预览链接和下载pdf文档时，均需带X-Stargate-App-Token查询参数，通过api网关访问授权后方可访问到内容，否则返回http 401。
    
### 3. 加签接口(文本，)
* 接口：POST /api/v1/countersign/text?source_url=xxx
* 功能：支持将pdf,word,ppt,excel等文档加签为pdf文档，并返回访问链接
* 入参：
    1. source_url：待转换文档url，此url中必须包含文件后缀名信息，目前可识别的文件后缀名有：.pdf, .doc, .docx, .ppt, .pptx, .xls, .xlsx
    2. 请求体
    ```json
  { 
    "content": "ch001", 
    "position_x": 10,
    "position_y": 20, 
    "font_size": 10, 
    "font_rgb_color": "rgb(0,0,0)", 
    "transparency": 80
  }
  ```
  content：加签内容，position_x：加签位置-x方向坐标，position_y：加签位置-y方向坐标，font_size：加签字体大小，font_rgb_color：加签文字颜色，必须为rgb(?,?,?)格式，?的值域范围为0-255，transparency：加签内容不透明度
* 出参：
    1. $.data.preview_url：预览链接(注：受永中dcs环境影响，pdf加签不支持preview_url预览)。
    2. $.data.pdf_url：转换后的pdf文档下载地址。
    3. 注意：访问预览链接和下载pdf文档时，均需带X-Stargate-App-Token查询参数，通过api网关访问授权后方可访问到内容，否则返回http 401。
    
### 4. 加水印
* 接口： POST /api/v1/watermark/text?source_url=xxx
* 功能：支持将pdf,word,ppt等文档转换pdf文档的时候，添加水印在文档中，最终返回访问链接
* 入参：
    1. source_url：待转换文档url，此url中必须包含文件后缀名信息，目前可识别的文件后缀名有：.pdf, .doc, .docx, .ppt, .pptx
    2. 请求体
    ```json
    {
    "content": "ch001", 
    "rotation_degree": 30,
    "font_size": 10, 
    "font_rgb_color": "rgb(0,0,0)", 
    "transparency": 0.5,
    "spacing_size": 100
    }
    ```
* 注意：pdf文档加水印，spacing_size功能不稳定（需找永中沟通技术细节，但由于目前无此需求，因此暂不处理）
  
### 5. 文档合并（支持word,pdf）
* 接口： POST /api/v1/merge
* 功能: 支持将多个pdf文档合并为一个pdf文档(受license限制，不支持多个word合并功能)
* 入参: 
    1. 请求体
    ```json
    [
      "http://www.xx.com/1.pdf",
      "http://www.xx.com/2.pdf"
    ]
    ```
* 出参: 
    1. $.data.preview_url：预览链接。
    2. $.data.pdf_url：转换后的pdf文档下载地址。
    4. 注意：访问预览链接和下载文档时，均需带X-Stargate-App-Token查询参数，通过api网关访问授权后方可访问到内容，否则返回http 401。
    
## 使用流程
### 1. 提交应用信息，获取访问token
1. 需提交`应用名称`及`应用描述信息`，以换取访问长虹文档转换服务接口的token信息。项目组应保证访问token的私密性，以防止token被非法冒用。
2. 若应用已在基础平台api网关服务中注册，则仅需告知应用id即可。

### 2. 使用长虹文档转换接口
1. 目前，应业务项目组需求，所有转换后的文档均会长期保存，即预览链接和pdf下载链接长久有效。
2. 业务项目组使用接口时，必须携带X-Stargate-App-Token查询参数，方有权限使用接口。

### 3. 合理使用转换后文档的访问链接
1. 长虹文档转换服务面向长虹应用提供文档转换服务，即：不管是文档转换接口还是转换后的文档访问链接，均服务于长虹应用，并不直接面向终端用户。
2. 转换后文档的访问权限控制原则上由各业务系统自行负责，以避免业务系统与能用平台间的耦合。
3. 推荐业务系统使用长虹基础平台`api网关`快速实现用户级的文档访问权限控制。