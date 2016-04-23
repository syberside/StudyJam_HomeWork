package ru.xrm.syber.currencyexchanger;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.util.Predicate;
import com.google.gson.Gson;
import com.squareup.okhttp.OkHttpClient;


import org.joda.time.DateTime;
import org.simpleframework.xml.Element;
import org.simpleframework.xml.Namespace;
import org.simpleframework.xml.NamespaceList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.convert.AnnotationStrategy;
import org.simpleframework.xml.core.Persister;
import org.simpleframework.xml.strategy.Strategy;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.RandomAccess;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import retrofit.Callback;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.client.OkClient;
import retrofit.client.Response;
import retrofit.converter.SimpleXMLConverter;
import retrofit.http.Body;
import retrofit.http.Headers;
import retrofit.http.POST;

public class MainActivity extends AppCompatActivity {

    List<CurrencyInfo> CurrencyInfo = new ArrayList<CurrencyInfo>();
    private EditText edit;
    private LinearLayout cursData;
    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        edit = (EditText)findViewById(R.id.value);
        cursData = (LinearLayout)findViewById(R.id.cursData);
        button = (Button)findViewById(R.id.button);
    }

    public void onClick(View v){
        if((cursData).getChildCount() > 0){
            (cursData).removeAllViews();
        }
        SetEnabled(false);
        showToast("Обновление данных с сайта ЦБРФ");

        Callback<Response> cb = new Callback<Response>() {

            @Override
            public void success(Response result, Response response) {
                String res = ResponseReader.readString(result);
                CurrencyInfo = CurrencyParser.parseXml(res);

                String asString = edit.getText().toString();
                Float value = asString==null || asString.equals("")?0:Float.valueOf(asString);

                for(int j = CurrencyInfo.size() - 1; j >= 0; j--){
                    CurrencyInfo info = CurrencyInfo.get(j);
                    Log.i("APP",info.toString());

                    LayoutInflater vi = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    View v = vi.inflate(R.layout.currency_item_layout, null);

                    // заполняем
                    TextView textView = (TextView) v.findViewById(R.id.Vname);
                    textView.setText(info.Vname);
                    textView = (TextView)v.findViewById(R.id.Vnom);
                    textView.setText(formatValute(info.Vnom,info.VchCode));
                    textView = (TextView)v.findViewById(R.id.Vcurs);
                    textView.setText(formatValute(info.Vcurs,"р."));
                    textView = (TextView)v.findViewById(R.id.calc);
                    Float calc = value*info.Vnom/info.Vcurs;
                    textView.setText(formatValute(calc,"р."));

                    // инжектим
                    ViewGroup insertPoint = (ViewGroup) cursData;
                    insertPoint.addView(v, 0, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.FILL_PARENT, ViewGroup.LayoutParams.FILL_PARENT));
                }
                SetEnabled(true);
                Log.i("APP", "success! "  + res);
                showToast("Данные обновлены!");
            }

            @Override
            public void failure(RetrofitError error) {
                SetEnabled(true);
                showToast("Ошибка при обновлении данных. Проверьте соединение с интернетом!");
                Log.e("APP", "failure! " + error.toString());
            }
        };

        GetCursOnDateXMLRequest data = new GetCursOnDateXMLRequest(new DateTime());
        RequestBody body = new RequestBody();
        body.setData(data);
        RequestEnvelope request = new RequestEnvelope();
        request.setRequestBody(body);

        RestAdapter restAdapter = getRestAdapter();
        SoapApi api = restAdapter.create(SoapApi.class);

        api.request(request, cb);
    }

    private void showToast(String result) {
        Toast toast = Toast.makeText(getBaseContext(),result,Toast.LENGTH_LONG);
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.show();
    }

    private void SetEnabled(boolean enabled) {
        button.setEnabled(enabled);
        edit.setEnabled(enabled);
    }

    public static RestAdapter getRestAdapter() {
        Strategy strategy = new AnnotationStrategy();
        Serializer serializer = new Persister(strategy);

        OkHttpClient okHttpClient = new OkHttpClient();
        RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint("http://www.cbr.ru")
                .setClient(new OkClient(okHttpClient))
                .setConverter(new SimpleXMLConverter(serializer))
                .setLogLevel(RestAdapter.LogLevel.FULL)
                .build();

        return restAdapter;
    }

    private static String formatValute(Float value, String valute){
        return String.format("%.2f", value)+" "+valute;
    }
}

class CurrencyParser{
    static public List<CurrencyInfo> parseXml(String res) {
        //TODO: parse by simple xml
        List<CurrencyInfo> infos = new ArrayList<CurrencyInfo>();
        CurrencyInfo usd = null;
        CurrencyInfo eur = null;
        try {
            InputSource source = new InputSource(new StringReader(res));
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(source);
            NodeList nodes = document.getDocumentElement().getElementsByTagName("ValuteCursOnDate");
            for(Node node:asList(nodes)){
                CurrencyInfo info = new CurrencyInfo();
                for(Node child:asList(node.getChildNodes())){
                    switch (child.getNodeName()){
                        case "Vname":
                            info.Vname = child.getTextContent().trim();
                            break;
                        case "VchCode":
                            info.VchCode = child.getTextContent().trim();
                            break;
                        case "Vcode":
                            info.Vcode = Integer.valueOf(child.getTextContent().trim());
                            break;
                        case "Vcurs":
                            info.Vcurs = Float.valueOf(child.getTextContent().trim());
                            break;
                        case "Vnom":
                            info.Vnom = Float.valueOf(child.getTextContent().trim());
                            break;
                    }
                }

                switch (info.VchCode){
                    case "USD":
                        usd = info;
                        break;
                    case "EUR":
                        eur = info;
                        break;
                    default:
                        infos.add(info);
                        break;
                }
            }

            Collections.sort(infos, new CompareByCode());

            if(eur!=null){
                infos.add(0,eur);
            }
            if(usd!=null){
                infos.add(0,usd);
            }
        }
        //TODO: catch concrete exceptions
        catch (Exception ex){
            ex.printStackTrace();
        }
        return infos;
    }

    private static List<Node> asList(NodeList n) {
        return n.getLength()==0?
                Collections.<Node>emptyList(): new NodeListWrapper(n);
    }

    static final class NodeListWrapper extends AbstractList<Node>
            implements RandomAccess {
        private final NodeList list;
        NodeListWrapper(NodeList l) {
            list=l;
        }
        public Node get(int index) {
            return list.item(index);
        }
        public int size() {
            return list.getLength();
        }
    }
}

class ResponseReader{
    public static String readString(Response result) {
        BufferedReader reader = null;
        StringBuilder sb = new StringBuilder();
        try {
            reader = new BufferedReader(new InputStreamReader(result.getBody().in()));
            String line;
            try {
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return sb.toString();
    }
}

class CurrencyInfo{
    public String Vname;
    public Float Vnom;
    public Float Vcurs;
    public Integer Vcode;
    public String VchCode;

    @Override
    public String toString(){
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}


//class LoadDataTask extends AsyncTask<Void, Void, String> {
//
//    private TextView v;
//    private Exception ex;
//
//    LoadDataTask(TextView v){
//
//        this.v = v;
//    }
//
//    @Override
//    protected String doInBackground(Void... voids) {
//        try{
//            Strategy strategy = new AnnotationStrategy();
//            Serializer serializer = new Persister(strategy);
//            OkHttpClient okHttpClient = new OkHttpClient();
//            String endpoint = "http://www.cbr.ru";
//            RestAdapter restAdapter = new RestAdapter.Builder()
//                    .setEndpoint(endpoint)
//                    .setClient(new OkClient(okHttpClient))
//                    .setConverter(new SimpleXMLConverter(serializer))
//                    .setLogLevel(RestAdapter.LogLevel.FULL)
//                    .build();
//            SoapApi api = restAdapter.create(SoapApi.class);
////            RequestBody body = RequestBody.create(MediaType.parse("text/xml; charset=utf-8"),
////                    "<GetCursOnDateXML xmlns=\"http://web.cbr.ru/\">\n" +
////                            "   <On_date>"+new DateTime().toString()+"</On_date>\n" +
////                            "</GetCursOnDateXML>");
//
//            GetCursOnDateXML pojo = new GetCursOnDateXML(new DateTime());
//            RequestBody body = new RequestBody(pojo);
//            RequestEnvelope env = new RequestEnvelope();
//            env.setBody(body);
//            String result = api.uploadRequest(env);
//            return result;
//        }
//        catch(Exception ex) {
//            this.ex = ex;
//            return null;
//        }
//    }
//
//    @Override
//    protected void onPostExecute(String feed) {
//        // TODO: check this.exception
//        // TODO: do something with the feed
//        if(this.ex!=null){
//            ex.printStackTrace();
//            v.setText(ex.toString());
//        }
//        else{
//            v.setText(feed);
//        }
//    }
//}

@Root(name = "soap12:Envelope")
@NamespaceList({
        @Namespace(reference = "http://www.w3.org/2001/XMLSchema-instance", prefix = "xsi"),
        @Namespace(reference = "http://www.w3.org/2001/XMLSchema", prefix = "xsd"),
        @Namespace(prefix = "soap12", reference = "http://www.w3.org/2003/05/soap-envelope")
})
class RequestEnvelope {
    @Element(name = "soap12:Body")
    private RequestBody body;

    public void setRequestBody(RequestBody body){
        this.body = body;
    }
}

@Root(name = "body", strict = false)
class RequestBody {
    @Element(name = "GetCursOnDateXML")
    private GetCursOnDateXMLRequest data;

    public void setData(GetCursOnDateXMLRequest data){
        this.data = data;
    }
}

@Root(name = "GetCursOnDateXML", strict = false)
@NamespaceList({
        @Namespace(reference = "http://web.cbr.ru/")
})
class GetCursOnDateXMLRequest {
    @Element(name = "On_date", required = false)
    private String onDate;

    public GetCursOnDateXMLRequest(DateTime onDateTime) {
        onDate = onDateTime.toString();
    }
}

interface SoapApi {
    @Headers({
            "Content-Type: application/soap+xml; charset=utf-8",
    })

    @POST("/DailyInfoWebServ/DailyInfo.asmx")
    public void request(@Body RequestEnvelope body, Callback<Response> cb);
}

class GetCursOnDateXMLResponse {
    @Element(name = "GetCursOnDateXMLResult", required = false)
    private GetCursOnDateXMLResult object;

    public GetCursOnDateXMLResponse() {
    }
}

class GetCursOnDateXMLResult {
    @Element(name = "GetCursOnDateXMLResult", required = false)
    private String object;

    public GetCursOnDateXMLResult() {
    }
}

@Root(name = Responce.ROOT_NAME, strict = false)
class Responce {
    public static final String ROOT_NAME = "GetCursOnDateXMLResponse";

    @Element(required = false)
    @Path("Body/GetCursOnDateXMLResponse/GetCursOnDateXMLResult/ValuteData/ValuteCursOnDate/")
    public String loginReturn;
}