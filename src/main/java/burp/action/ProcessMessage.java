package burp.action;

import burp.IExtensionHelpers;
import burp.IHttpService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProcessMessage {
    MatchHTTP mh = new MatchHTTP();
    ExtractContent ec = new ExtractContent();
    DoAction da = new DoAction();
    GetColorKey gck = new GetColorKey();
    UpgradeColor uc = new UpgradeColor();

    public List<String> processMessageByContent(IExtensionHelpers helpers, byte[] content, boolean isRequest, boolean messageInfo) {
        List<String> result = new ArrayList<>();;
        Map<String, Map<String, Object>> obj;

        if (isRequest) {

            // 获取报文头
            List<String> requestTmpHeaders = helpers.analyzeRequest(content).getHeaders();
            String requestHeaders = String.join("\n", requestTmpHeaders);

            try {
                // 流量清洗
                String urlString = requestTmpHeaders.get(0).split(" ")[1];
                urlString = urlString.indexOf("?") > 0 ? urlString.substring(0, urlString.indexOf("?")) : urlString;

                // 正则判断
                if (mh.matchSuffix(urlString)) {
                    return result;
                }
            } catch (Exception e) {
                return result;
            }


            // 获取报文主体
            int requestBodyOffset = helpers.analyzeRequest(content).getBodyOffset();
            byte[] requestBody = Arrays.copyOfRange(content, requestBodyOffset, content.length);

            obj = ec.matchRegex(content, requestHeaders, requestBody, "request");
        } else {
            try {
                // 流量清洗
                String inferredMimeType = String.format("hae.%s", helpers.analyzeResponse(content).getInferredMimeType().toLowerCase());
                String statedMimeType = String.format("hae.%s", helpers.analyzeResponse(content).getStatedMimeType().toLowerCase());
                // 正则判断
                if (mh.matchSuffix(statedMimeType) || mh.matchSuffix(inferredMimeType)) {
                    return result;
                }
            } catch (Exception e) {
                return result;
            }
            // 获取报文头
            List<String> responseTmpHeaders = helpers.analyzeResponse(content).getHeaders();
            String responseHeaders = String.join("\n", responseTmpHeaders);

            // 获取报文主体
            int responseBodyOffset = helpers.analyzeResponse(content).getBodyOffset();
            byte[] responseBody = Arrays.copyOfRange(content, responseBodyOffset, content.length);

            obj = ec.matchRegex(content, responseHeaders, responseBody, "response");
        }

        if (messageInfo) {
            List<List<String>> resultList = da.highlightAndComment(obj);
            List<String> colorList = resultList.get(0);
            List<String> commentList = resultList.get(1);
            if (colorList.size() != 0 && commentList.size() != 0) {
                String color = uc.getEndColor(gck.getColorKeys(colorList));
                result.add(color);
                result.add(String.join(", ", commentList));
            }
        } else {
            if (obj.size() > 0) {
                result.add(da.extractString(obj));
            }
        }

        return result;

    }
}