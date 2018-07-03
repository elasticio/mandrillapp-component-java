package io.elastic.mandrill.triggers

import com.github.restdriver.clientdriver.ClientDriverRequest
import com.github.restdriver.clientdriver.ClientDriverRule
import io.elastic.api.*
import io.elastic.mandrill.Constants
import org.junit.ClassRule
import spock.lang.Shared
import spock.lang.Specification

import javax.json.Json

import static com.github.restdriver.clientdriver.RestClientDriver.giveResponse
import static com.github.restdriver.clientdriver.RestClientDriver.onRequestTo
import static org.hamcrest.Matchers.equalToIgnoringCase

class WebhookEventsSpecification extends Specification {

    @ClassRule
    @Shared
    public ClientDriverRule driver = new ClientDriverRule(11111);

    def setupSpec() {

        System.setProperty(Constants.ENV_VAR_WEBHOOK_URI, "http://example/webhook-url")
        System.setProperty(Constants.ENV_VAR_MANDRILL_API_BASE_URL, driver.getBaseUrl())

        def webhookResponse = Json.createObjectBuilder()
                .add("id", "42")
                .add("url", "http://example/webhook-url")
                .build();

        driver.addExpectation(
                onRequestTo(Constants.MANDRILL_API_WEBBHOOKS_ADD_PATH)
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBody(equalToIgnoringCase('{"key":"super-secret","url":"http://example/webhook-url"}'), "application/json"),
                giveResponse(
                        JSON.stringify(webhookResponse),
                        'application/json')
                        .withStatus(200)).anyTimes()

        driver.addExpectation(
                onRequestTo(Constants.MANDRILL_API_WEBBHOOKS_DELETE_PATH)
                        .withMethod(ClientDriverRequest.Method.POST)
                        .withBody(equalToIgnoringCase('{"key":"super-secret","id":"105"}'), "application/json"),
                giveResponse(
                        JSON.stringify(webhookResponse),
                        'application/json')
                        .withStatus(200)).anyTimes()
    }

    def "startup"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        def startupParameters = new StartupParameters.Builder()
                .configuration(configuration)
                .build()


        when:
        def result = events.startup(startupParameters)

        then:
        JSON.stringify(result) == "{\"id\":\"42\",\"url\":\"http://example/webhook-url\"}"
    }

    def "shutdown"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        def state = Json.createObjectBuilder()
                .add("id", "105")
                .build();

        def shutdownParameters = new ShutdownParameters.Builder()
                .configuration(configuration)
                .state(state)
                .build();


        when:
        events.shutdown(shutdownParameters)

        then:
        true
    }

    def "execute"() {

        setup:
        def events = new WebhookEvents()

        def configuration = Json.createObjectBuilder()
                .add(Constants.CONFIGURATION_API_KEY, "super-secret")
                .build();

        EventEmitter.Callback errorCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback snapshotCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback dataCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback reboundCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback updateKeysCallback = Mock(EventEmitter.Callback)
        EventEmitter.Callback httpReplyCallback = Mock(EventEmitter.Callback)
        EventEmitter emitter = new EventEmitter(errorCallback, dataCallback, snapshotCallback,
                reboundCallback, updateKeysCallback, httpReplyCallback)

        def mandrillEvents = "%5B%7B%22event%22%3A%22inbound%22%2C%22ts%22%3A1503928695%2C%22msg%22%3A%7B%22raw_msg%22%3A%22Received%3A+from+mail-wr0-f177.google.com+%28unknown+%5B209.85.128.177%5D%29%5Cn%5Ctby+relay-3.us-west-2.relay-prod+%28Postfix%29+with+ESMTPS+id+5CFE72002BA%5Cn%5Ctfor+%3Cin-sales%40m.foo.nar%3E%3B+Mon%2C+28+Aug+2017+13%3A58%3A23+%2B0000+%28UTC%29%5CnReceived%3A+by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5%5Cn++++++++for+%3Cin-sales%40m.foo.nar%3E%3B+Mon%2C+28+Aug+2017+06%3A58%3A23+-0700+%28PDT%29%5CnDKIM-Signature%3A+v%3D1%3B+a%3Drsa-sha256%3B+c%3Drelaxed%5C%2Frelaxed%3B%5Cn++++++++d%3Dfoo.nar%3B+s%3Dgoogle%3B%5Cn++++++++h%3Dmime-version%3Ain-reply-to%3Areferences%3Afrom%3Adate%3Amessage-id%3Asubject%3Ato%3B%5Cn++++++++bh%3DYxAgQTheXoVRva5xHCxk5ez%2BBWqEau%5C%2FFhNuT0%5C%2FFHbhI%3D%3B%5Cn++++++++b%3DWPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ%5C%2FdDDJMJrYLL02Kz1CZi%5Cn+++++++++50FJG31E0rci%2B8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2%5Cn+++++++++aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw%3D%5CnX-Google-DKIM-Signature%3A+v%3D1%3B+a%3Drsa-sha256%3B+c%3Drelaxed%5C%2Frelaxed%3B%5Cn++++++++d%3D1e100.net%3B+s%3D20161025%3B%5Cn++++++++h%3Dx-gm-message-state%3Amime-version%3Ain-reply-to%3Areferences%3Afrom%3Adate%5Cn+++++++++%3Amessage-id%3Asubject%3Ato%3B%5Cn++++++++bh%3DYxAgQTheXoVRva5xHCxk5ez%2BBWqEau%5C%2FFhNuT0%5C%2FFHbhI%3D%3B%5Cn++++++++b%3DQm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0%5Cn+++++++++hwCixy3k7sX3cvSCVscyV9sk8%5C%2Fi%5C%2FICZjZuhDsOI%5C%2F3IaM%2BDDy%5C%2FHvU%5C%2FzSK0xJuW8hUtcqB%5Cn+++++++++Za%5C%2F3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ%2BDxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR%5Cn+++++++++uPdiiDZZrJt706rjRwSDsa5Yx%5C%2Fi%2BN3IB31bM5hId8oKVVJTDSzLN%2BQCnneK3OO63jEuA%5Cn+++++++++1MN71XxIuhMNPFbV1Pdj0Flfx7Quag%2BnP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk%5Cn+++++++++fXbg%3D%3D%5CnX-Gm-Message-State%3A+AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE%5C%2F4OayVN4HIeJ20aFa9Mm5ui%5Cn%5CtrMkaN%5C%2FlLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg%3D%3D%5CnX-Received%3A+by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697%3B%5Cn+Mon%2C+28+Aug+2017+06%3A58%3A21+-0700+%28PDT%29%5CnMIME-Version%3A+1.0%5CnReceived%3A+by+10.223.198.9+with+HTTP%3B+Mon%2C+28+Aug+2017+06%3A58%3A01+-0700+%28PDT%29%5CnX-Originating-IP%3A+%5B84.163.64.106%5D%5CnIn-Reply-To%3A+%3C9196e4ebe211e67e995df6faba3b111e%40www.foo.nar%3E%5CnReferences%3A+%3C9196e4ebe211e67e995df6faba3b111e%40www.foo.nar%3E%5CnFrom%3A+John+Snow+%3Cjohn.snow%40foo.nar%3E%5CnDate%3A+Mon%2C+28+Aug+2017+15%3A58%3A01+%2B0200%5CnMessage-ID%3A+%3CCA%2BLhcb8J6dO%3D1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A%40mail.gmail.com%3E%5CnSubject%3A+Fwd%3A+New+Lead+from+Trial+Request+Page%5CnTo%3A+in-sales%40m.foo.nar%5CnContent-Type%3A+multipart%5C%2Falternative%3B+boundary%3D%5C%22089e0824170c0856720557d0b076%5C%22%5Cn%5Cn--089e0824170c0856720557d0b076%5CnContent-Type%3A+text%5C%2Fplain%3B+charset%3D%5C%22UTF-8%5C%22%5Cn%5CnNew+lead+has+been+captured+from+the+Trial+Request+page%21%5Cn%5CnLead%27s+name%3A+John+Doe%5CnContact+email%3A+johnd%40HBO.cz%5CnContact+phone+number%3A%5CnPosition%3A+Network+specialist%5CnCompany%3A+HBO+Group+s.r.o%5CnCompany+Size%3A+1+-+10%5Cn%5CnSubmitted+message%3A%5CnEmails+and+MicrosoftSQL+...%5Cn%5Cn%5Cn---------%5CnDebug+Information+about+Submitter%3A%5Cn%5CnIP+address+is%3A+84.42.179.2%5CnUser+Agent+is%3A+Mozilla%5C%2F5.0+%28Windows+NT+10.0%3B+Win64%3B+x64%29+AppleWebKit%5C%2F537.36%5Cn%28KHTML%2C+like+Gecko%29+Chrome%5C%2F60.0.3112.101+Safari%5C%2F537.36%5CnPage+URL+is%3A+++https%3A%5C%2F%5C%2Fwww.foo.nar%5C%2Frequest-trial%5C%2F%5CnSubmission+Date+is%3A+25%5C%2F08%5C%2F2017%5CnSubmission+Time+is%3A+11%3A54%5CnHidden+field+called+ga_ID+has+value%3A+2055698932.1503652393%5Cn%5Cn--089e0824170c0856720557d0b076%5CnContent-Type%3A+text%5C%2Fhtml%3B+charset%3D%5C%22UTF-8%5C%22%5CnContent-Transfer-Encoding%3A+quoted-printable%5Cn%5Cn%3Cdiv+dir%3D3D%5C%22ltr%5C%22%3E%3Cdiv+class%3D3D%5C%22gmail_quote%5C%22%3ENew+lead+has+been+captured+from%3D%5Cn+the+Trial+Request+page%21%3Cbr%3E%5Cn%3Cbr%3E%5CnLead%26%2339%3Bs+name%3A+John+Doe%3Cbr%3E%5CnContact+email%3A+%3Ca+href%3D3D%5C%22mailto%3Ajohnd%40HBO.cz%5C%22%3Ejohnd%40HBO.cz%3C%5C%2Fa%3E%3Cb%3D%5Cnr%3E%5CnContact+phone+number%3A%3Cbr%3E%5CnPosition%3A+Network+specialist%3Cbr%3E%5CnCompany%3A+HBO+Group+s.r.o%3Cbr%3E%5CnCompany+Size%3A+1+-+10%3Cbr%3E%5Cn%3Cbr%3E%5CnSubmitted+message%3A%3Cbr%3E%5CnEmails+and+MicrosoftSQL+...%3Cbr%3E%5Cn%3Cbr%3E%5Cn%3Cbr%3E%5Cn---------%3Cbr%3E%5CnDebug+Information+about+Submitter%3A%3Cbr%3E%5Cn%3Cbr%3E%5CnIP+address+is%3A+84.42.179.2%3Cbr%3E%5CnUser+Agent+is%3A+Mozilla%5C%2F5.0+%28Windows+NT+10.0%3B+Win64%3B+x64%29+AppleWebKit%5C%2F537.36%3D%5Cn+%28KHTML%2C+like+Gecko%29+Chrome%5C%2F60.0.3112.101+Safari%5C%2F537.36%3Cbr%3E%5CnPage+URL+is%3A%3DC2%3DA0+%3DC2%3DA0%3Ca+href%3D3D%5C%22https%3A%5C%2F%5C%2Fwww.foo.nar%5C%2Frequest-trial%5C%2F%5C%22+%3D%5Cnrel%3D3D%5C%22noreferrer%5C%22+target%3D3D%5C%22_blank%5C%22%3Ehttps%3A%5C%2F%5C%2Fwww.foo.nar%5C%2F%3Cwbr%3Erequest-tr%3D%5Cnial%5C%2F%3C%5C%2Fa%3E%3Cbr%3E%5CnSubmission+Date+is%3A+25%5C%2F08%5C%2F2017%3Cbr%3E%5CnSubmission+Time+is%3A+11%3A54%3Cbr%3E%5CnHidden+field+called+ga_ID+has+value%3A+%3Ca+href%3D3D%5C%22tel%3A2055698932%5C%22+value%3D3D%5C%22%2B1%3D%5Cn2055698932%5C%22%3E2055698932%3C%5C%2Fa%3E.1503652393%3Cbr%3E%5Cn%3C%5C%2Fdiv%3E%3Cbr%3E%3C%5C%2Fdiv%3E%5Cn%5Cn--089e0824170c0856720557d0b076--%22%2C%22headers%22%3A%7B%22Received%22%3A%5B%22from+mail-wr0-f177.google.com+%28unknown+%5B209.85.128.177%5D%29+by+relay-3.us-west-2.relay-prod+%28Postfix%29+with+ESMTPS+id+5CFE72002BA+for+%3Cin-sales%40m.foo.nar%3E%3B+Mon%2C+28+Aug+2017+13%3A58%3A23+%2B0000+%28UTC%29%22%2C%22by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5+for+%3Cin-sales%40m.foo.nar%3E%3B+Mon%2C+28+Aug+2017+06%3A58%3A23+-0700+%28PDT%29%22%2C%22by+10.223.198.9+with+HTTP%3B+Mon%2C+28+Aug+2017+06%3A58%3A01+-0700+%28PDT%29%22%5D%2C%22Dkim-Signature%22%3A%22v%3D1%3B+a%3Drsa-sha256%3B+c%3Drelaxed%5C%2Frelaxed%3B+d%3Dfoo.nar%3B+s%3Dgoogle%3B+h%3Dmime-version%3Ain-reply-to%3Areferences%3Afrom%3Adate%3Amessage-id%3Asubject%3Ato%3B+bh%3DYxAgQTheXoVRva5xHCxk5ez%2BBWqEau%5C%2FFhNuT0%5C%2FFHbhI%3D%3B+b%3DWPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ%5C%2FdDDJMJrYLL02Kz1CZi+50FJG31E0rci%2B8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2+aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw%3D%22%2C%22X-Google-Dkim-Signature%22%3A%22v%3D1%3B+a%3Drsa-sha256%3B+c%3Drelaxed%5C%2Frelaxed%3B+d%3D1e100.net%3B+s%3D20161025%3B+h%3Dx-gm-message-state%3Amime-version%3Ain-reply-to%3Areferences%3Afrom%3Adate+%3Amessage-id%3Asubject%3Ato%3B+bh%3DYxAgQTheXoVRva5xHCxk5ez%2BBWqEau%5C%2FFhNuT0%5C%2FFHbhI%3D%3B+b%3DQm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0+hwCixy3k7sX3cvSCVscyV9sk8%5C%2Fi%5C%2FICZjZuhDsOI%5C%2F3IaM%2BDDy%5C%2FHvU%5C%2FzSK0xJuW8hUtcqB+Za%5C%2F3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ%2BDxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR+uPdiiDZZrJt706rjRwSDsa5Yx%5C%2Fi%2BN3IB31bM5hId8oKVVJTDSzLN%2BQCnneK3OO63jEuA+1MN71XxIuhMNPFbV1Pdj0Flfx7Quag%2BnP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk+fXbg%3D%3D%22%2C%22X-Gm-Message-State%22%3A%22AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE%5C%2F4OayVN4HIeJ20aFa9Mm5ui+rMkaN%5C%2FlLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg%3D%3D%22%2C%22X-Received%22%3A%22by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697%3B+Mon%2C+28+Aug+2017+06%3A58%3A21+-0700+%28PDT%29%22%2C%22Mime-Version%22%3A%221.0%22%2C%22X-Originating-Ip%22%3A%22%5B84.163.64.106%5D%22%2C%22In-Reply-To%22%3A%22%3C9196e4ebe211e67e995df6faba3b111e%40www.foo.nar%3E%22%2C%22References%22%3A%22%3C9196e4ebe211e67e995df6faba3b111e%40www.foo.nar%3E%22%2C%22From%22%3A%22John+Snow+%3Cjohn.snow%40foo.nar%3E%22%2C%22Date%22%3A%22Mon%2C+28+Aug+2017+15%3A58%3A01+%2B0200%22%2C%22Message-Id%22%3A%22%3CCA%2BLhcb8J6dO%3D1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A%40mail.gmail.com%3E%22%2C%22Subject%22%3A%22Fwd%3A+New+Lead+from+Trial+Request+Page%22%2C%22To%22%3A%22in-sales%40m.foo.nar%22%2C%22Content-Type%22%3A%22multipart%5C%2Falternative%3B+boundary%3D%5C%22089e0824170c0856720557d0b076%5C%22%22%7D%2C%22text%22%3A%22New+lead+has+been+captured+from+the+Trial+Request+page%21%5Cn%5CnLead%27s+name%3A+John+Doe%5CnContact+email%3A+johnd%40HBO.cz%5CnContact+phone+number%3A%5CnPosition%3A+Network+specialist%5CnCompany%3A+HBO+Group+s.r.o%5CnCompany+Size%3A+1+-+10%5Cn%5CnSubmitted+message%3A%5CnEmails+and+MicrosoftSQL+...%5Cn%5Cn%5Cn---------%5CnDebug+Information+about+Submitter%3A%5Cn%5CnIP+address+is%3A+84.42.179.2%5CnUser+Agent+is%3A+Mozilla%5C%2F5.0+%28Windows+NT+10.0%3B+Win64%3B+x64%29+AppleWebKit%5C%2F537.36%5Cn%28KHTML%2C+like+Gecko%29+Chrome%5C%2F60.0.3112.101+Safari%5C%2F537.36%5CnPage+URL+is%3A+++https%3A%5C%2F%5C%2Fwww.foo.nar%5C%2Frequest-trial%5C%2F%5CnSubmission+Date+is%3A+25%5C%2F08%5C%2F2017%5CnSubmission+Time+is%3A+11%3A54%5CnHidden+field+called+ga_ID+has+value%3A+2055698932.1503652393%5Cn%5Cn%22%2C%22text_flowed%22%3Afalse%2C%22html%22%3A%22%3Cdiv+dir%3D%5C%22ltr%5C%22%3E%3Cdiv+class%3D%5C%22gmail_quote%5C%22%3ENew+lead+has+been+captured+from+the+Trial+Request+page%21%3Cbr%3E%5Cn%3Cbr%3E%5CnLead%26%2339%3Bs+name%3A+John+Doe%3Cbr%3E%5CnContact+email%3A+%3Ca+href%3D%5C%22mailto%3Ajohnd%40HBO.cz%5C%22%3Ejohnd%40HBO.cz%3C%5C%2Fa%3E%3Cbr%3E%5CnContact+phone+number%3A%3Cbr%3E%5CnPosition%3A+Network+specialist%3Cbr%3E%5CnCompany%3A+HBO+Group+s.r.o%3Cbr%3E%5CnCompany+Size%3A+1+-+10%3Cbr%3E%5Cn%3Cbr%3E%5CnSubmitted+message%3A%3Cbr%3E%5CnEmails+and+MicrosoftSQL+...%3Cbr%3E%5Cn%3Cbr%3E%5Cn%3Cbr%3E%5Cn---------%3Cbr%3E%5CnDebug+Information+about+Submitter%3A%3Cbr%3E%5Cn%3Cbr%3E%5CnIP+address+is%3A+84.42.179.2%3Cbr%3E%5CnUser+Agent+is%3A+Mozilla%5C%2F5.0+%28Windows+NT+10.0%3B+Win64%3B+x64%29+AppleWebKit%5C%2F537.36+%28KHTML%2C+like+Gecko%29+Chrome%5C%2F60.0.3112.101+Safari%5C%2F537.36%3Cbr%3E%5CnPage+URL+is%3A%5Cu00a0+%5Cu00a0%3Ca+href%3D%5C%22https%3A%5C%2F%5C%2Fwww.foo.nar%5C%2Frequest-trial%5C%2F%5C%22+rel%3D%5C%22noreferrer%5C%22+target%3D%5C%22_blank%5C%22%3Ehttps%3A%5C%2F%5C%2Fwww.foo.nar%5C%2F%3Cwbr%3Erequest-trial%5C%2F%3C%5C%2Fa%3E%3Cbr%3E%5CnSubmission+Date+is%3A+25%5C%2F08%5C%2F2017%3Cbr%3E%5CnSubmission+Time+is%3A+11%3A54%3Cbr%3E%5CnHidden+field+called+ga_ID+has+value%3A+%3Ca+href%3D%5C%22tel%3A2055698932%5C%22+value%3D%5C%22%2B12055698932%5C%22%3E2055698932%3C%5C%2Fa%3E.1503652393%3Cbr%3E%5Cn%3C%5C%2Fdiv%3E%3Cbr%3E%3C%5C%2Fdiv%3E%5Cn%5Cn%22%2C%22from_email%22%3A%22john.snow%40foo.nar%22%2C%22from_name%22%3A%22John+Snow%22%2C%22to%22%3A%5B%5B%22in-sales%40m.foo.nar%22%2Cnull%5D%5D%2C%22subject%22%3A%22Fwd%3A+New+Lead+from+Trial+Request+Page%22%2C%22spf%22%3A%7B%22result%22%3A%22pass%22%2C%22detail%22%3A%22sender+SPF+authorized%22%7D%2C%22spam_report%22%3A%7B%22score%22%3A1.2%2C%22matched_rules%22%3A%5B%7B%22name%22%3A%22URIBL_BLOCKED%22%2C%22score%22%3A0%2C%22description%22%3A%22ADMINISTRATOR+NOTICE%3A+The+query+to+URIBL+was+blocked.%22%7D%2C%7B%22name%22%3Anull%2C%22score%22%3A0%2C%22description%22%3Anull%7D%2C%7B%22name%22%3A%22more%22%2C%22score%22%3A0%2C%22description%22%3A%22information.%22%7D%2C%7B%22name%22%3A%22HBO.cz%5D%22%2C%22score%22%3A0%2C%22description%22%3Anull%7D%2C%7B%22name%22%3A%22RCVD_IN_DNSWL_NONE%22%2C%22score%22%3A-0%2C%22description%22%3A%22RBL%3A+Sender+listed+at+http%3A%5C%2F%5C%2Fwww.dnswl.org%5C%2F%2C+no%22%7D%2C%7B%22name%22%3A%22listed%22%2C%22score%22%3A0%2C%22description%22%3A%22in+list.dnswl.org%5D%22%7D%2C%7B%22name%22%3A%22HTML_MESSAGE%22%2C%22score%22%3A0%2C%22description%22%3A%22BODY%3A+HTML+included+in+message%22%7D%2C%7B%22name%22%3A%22DKIM_VALID_AU%22%2C%22score%22%3A-0.1%2C%22description%22%3A%22Message+has+a+valid+DKIM+or+DK+signature+from+author%27s%22%7D%2C%7B%22name%22%3A%22DKIM_SIGNED%22%2C%22score%22%3A0.1%2C%22description%22%3A%22Message+has+a+DKIM+or+DK+signature%2C+not+necessarily+valid%22%7D%2C%7B%22name%22%3A%22DKIM_VALID%22%2C%22score%22%3A-0.1%2C%22description%22%3A%22Message+has+at+least+one+valid+DKIM+or+DK+signature%22%7D%2C%7B%22name%22%3A%22RDNS_NONE%22%2C%22score%22%3A1.3%2C%22description%22%3A%22Delivered+to+internal+network+by+a+host+with+no+rDNS%22%7D%2C%7B%22name%22%3A%22T_FILL_THIS_FORM_SHORT%22%2C%22score%22%3A0%2C%22description%22%3A%22Fill+in+a+short+form+with+personal+information%22%7D%5D%7D%2C%22dkim%22%3A%7B%22signed%22%3Atrue%2C%22valid%22%3Atrue%7D%2C%22email%22%3A%22in-sales%40m.foo.nar%22%2C%22tags%22%3A%5B%5D%2C%22sender%22%3Anull%2C%22template%22%3Anull%7D%7D%5D"

        def body = Json.createObjectBuilder()
                .add("mandrill_events", mandrillEvents)
                .build()

        def incomingMessage = new Message.Builder().body(body).build()

        def parameters = new ExecutionParameters.Builder(incomingMessage, emitter)
                .configuration(configuration)
                .build()


        when:
        events.execute(parameters)

        then:
        then:
        1 * dataCallback.receive({
            JSON.stringify(it.getBody()) == '{"event":"inbound","ts":1503928695,"msg":{"raw_msg":"Received: from mail-wr0-f177.google.com (unknown [209.85.128.177])\\n\\tby relay-3.us-west-2.relay-prod (Postfix) with ESMTPS id 5CFE72002BA\\n\\tfor <in-sales@m.foo.nar>; Mon, 28 Aug 2017 13:58:23 +0000 (UTC)\\nReceived: by mail-wr0-f177.google.com with SMTP id 40so1418592wrv.5\\n        for <in-sales@m.foo.nar>; Mon, 28 Aug 2017 06:58:23 -0700 (PDT)\\nDKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed;\\n        d=foo.nar; s=google;\\n        h=mime-version:in-reply-to:references:from:date:message-id:subject:to;\\n        bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;\\n        b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ/dDDJMJrYLL02Kz1CZi\\n         50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2\\n         aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=\\nX-Google-DKIM-Signature: v=1; a=rsa-sha256; c=relaxed/relaxed;\\n        d=1e100.net; s=20161025;\\n        h=x-gm-message-state:mime-version:in-reply-to:references:from:date\\n         :message-id:subject:to;\\n        bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;\\n        b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0\\n         hwCixy3k7sX3cvSCVscyV9sk8/i/ICZjZuhDsOI/3IaM+DDy/HvU/zSK0xJuW8hUtcqB\\n         Za/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR\\n         uPdiiDZZrJt706rjRwSDsa5Yx/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA\\n         1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk\\n         fXbg==\\nX-Gm-Message-State: AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE/4OayVN4HIeJ20aFa9Mm5ui\\n\\trMkaN/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==\\nX-Received: by 10.223.197.133 with SMTP id m5mr558062wrg.276.1503928701697;\\n Mon, 28 Aug 2017 06:58:21 -0700 (PDT)\\nMIME-Version: 1.0\\nReceived: by 10.223.198.9 with HTTP; Mon, 28 Aug 2017 06:58:01 -0700 (PDT)\\nX-Originating-IP: [84.163.64.106]\\nIn-Reply-To: <9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nReferences: <9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nFrom: John Snow <john.snow@foo.nar>\\nDate: Mon, 28 Aug 2017 15:58:01 +0200\\nMessage-ID: <CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>\\nSubject: Fwd: New Lead from Trial Request Page\\nTo: in-sales@m.foo.nar\\nContent-Type: multipart/alternative; boundary=\\"089e0824170c0856720557d0b076\\"\\n\\n--089e0824170c0856720557d0b076\\nContent-Type: text/plain; charset=\\"UTF-8\\"\\n\\nNew lead has been captured from the Trial Request page!\\n\\nLead\'s name: John Doe\\nContact email: johnd@HBO.cz\\nContact phone number:\\nPosition: Network specialist\\nCompany: HBO Group s.r.o\\nCompany Size: 1 - 10\\n\\nSubmitted message:\\nEmails and MicrosoftSQL ...\\n\\n\\n---------\\nDebug Information about Submitter:\\n\\nIP address is: 84.42.179.2\\nUser Agent is: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\\n(KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36\\nPage URL is:   https://www.foo.nar/request-trial/\\nSubmission Date is: 25/08/2017\\nSubmission Time is: 11:54\\nHidden field called ga_ID has value: 2055698932.1503652393\\n\\n--089e0824170c0856720557d0b076\\nContent-Type: text/html; charset=\\"UTF-8\\"\\nContent-Transfer-Encoding: quoted-printable\\n\\n<div dir=3D\\"ltr\\"><div class=3D\\"gmail_quote\\">New lead has been captured from=\\n the Trial Request page!<br>\\n<br>\\nLead&#39;s name: John Doe<br>\\nContact email: <a href=3D\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz</a><b=\\nr>\\nContact phone number:<br>\\nPosition: Network specialist<br>\\nCompany: HBO Group s.r.o<br>\\nCompany Size: 1 - 10<br>\\n<br>\\nSubmitted message:<br>\\nEmails and MicrosoftSQL ...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug Information about Submitter:<br>\\n<br>\\nIP address is: 84.42.179.2<br>\\nUser Agent is: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36=\\n (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36<br>\\nPage URL is:=C2=A0 =C2=A0<a href=3D\\"https://www.foo.nar/request-trial/\\" =\\nrel=3D\\"noreferrer\\" target=3D\\"_blank\\">https://www.foo.nar/<wbr>request-tr=\\nial/</a><br>\\nSubmission Date is: 25/08/2017<br>\\nSubmission Time is: 11:54<br>\\nHidden field called ga_ID has value: <a href=3D\\"tel:2055698932\\" value=3D\\"+1=\\n2055698932\\">2055698932</a>.1503652393<br>\\n</div><br></div>\\n\\n--089e0824170c0856720557d0b076--","headers":{"Received":["from mail-wr0-f177.google.com (unknown [209.85.128.177]) by relay-3.us-west-2.relay-prod (Postfix) with ESMTPS id 5CFE72002BA for <in-sales@m.foo.nar>; Mon, 28 Aug 2017 13:58:23 +0000 (UTC)","by mail-wr0-f177.google.com with SMTP id 40so1418592wrv.5 for <in-sales@m.foo.nar>; Mon, 28 Aug 2017 06:58:23 -0700 (PDT)","by 10.223.198.9 with HTTP; Mon, 28 Aug 2017 06:58:01 -0700 (PDT)"],"Dkim-Signature":"v=1; a=rsa-sha256; c=relaxed/relaxed; d=foo.nar; s=google; h=mime-version:in-reply-to:references:from:date:message-id:subject:to; bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=; b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ/dDDJMJrYLL02Kz1CZi 50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2 aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=","X-Google-Dkim-Signature":"v=1; a=rsa-sha256; c=relaxed/relaxed; d=1e100.net; s=20161025; h=x-gm-message-state:mime-version:in-reply-to:references:from:date :message-id:subject:to; bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=; b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0 hwCixy3k7sX3cvSCVscyV9sk8/i/ICZjZuhDsOI/3IaM+DDy/HvU/zSK0xJuW8hUtcqB Za/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR uPdiiDZZrJt706rjRwSDsa5Yx/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA 1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk fXbg==","X-Gm-Message-State":"AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE/4OayVN4HIeJ20aFa9Mm5ui rMkaN/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==","X-Received":"by 10.223.197.133 with SMTP id m5mr558062wrg.276.1503928701697; Mon, 28 Aug 2017 06:58:21 -0700 (PDT)","Mime-Version":"1.0","X-Originating-Ip":"[84.163.64.106]","In-Reply-To":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","References":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","From":"John Snow <john.snow@foo.nar>","Date":"Mon, 28 Aug 2017 15:58:01 +0200","Message-Id":"<CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>","Subject":"Fwd: New Lead from Trial Request Page","To":"in-sales@m.foo.nar","Content-Type":"multipart/alternative; boundary=\\"089e0824170c0856720557d0b076\\""},"text":"New lead has been captured from the Trial Request page!\\n\\nLead\'s name: John Doe\\nContact email: johnd@HBO.cz\\nContact phone number:\\nPosition: Network specialist\\nCompany: HBO Group s.r.o\\nCompany Size: 1 - 10\\n\\nSubmitted message:\\nEmails and MicrosoftSQL ...\\n\\n\\n---------\\nDebug Information about Submitter:\\n\\nIP address is: 84.42.179.2\\nUser Agent is: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36\\n(KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36\\nPage URL is:   https://www.foo.nar/request-trial/\\nSubmission Date is: 25/08/2017\\nSubmission Time is: 11:54\\nHidden field called ga_ID has value: 2055698932.1503652393\\n\\n","text_flowed":false,"html":"<div dir=\\"ltr\\"><div class=\\"gmail_quote\\">New lead has been captured from the Trial Request page!<br>\\n<br>\\nLead&#39;s name: John Doe<br>\\nContact email: <a href=\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz</a><br>\\nContact phone number:<br>\\nPosition: Network specialist<br>\\nCompany: HBO Group s.r.o<br>\\nCompany Size: 1 - 10<br>\\n<br>\\nSubmitted message:<br>\\nEmails and MicrosoftSQL ...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug Information about Submitter:<br>\\n<br>\\nIP address is: 84.42.179.2<br>\\nUser Agent is: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/60.0.3112.101 Safari/537.36<br>\\nPage URL is:   <a href=\\"https://www.foo.nar/request-trial/\\" rel=\\"noreferrer\\" target=\\"_blank\\">https://www.foo.nar/<wbr>request-trial/</a><br>\\nSubmission Date is: 25/08/2017<br>\\nSubmission Time is: 11:54<br>\\nHidden field called ga_ID has value: <a href=\\"tel:2055698932\\" value=\\"+12055698932\\">2055698932</a>.1503652393<br>\\n</div><br></div>\\n\\n","from_email":"john.snow@foo.nar","from_name":"John Snow","to":[["in-sales@m.foo.nar",null]],"subject":"Fwd: New Lead from Trial Request Page","spf":{"result":"pass","detail":"sender SPF authorized"},"spam_report":{"score":1.2,"matched_rules":[{"name":"URIBL_BLOCKED","score":0,"description":"ADMINISTRATOR NOTICE: The query to URIBL was blocked."},{"name":null,"score":0,"description":null},{"name":"more","score":0,"description":"information."},{"name":"HBO.cz]","score":0,"description":null},{"name":"RCVD_IN_DNSWL_NONE","score":0,"description":"RBL: Sender listed at http://www.dnswl.org/, no"},{"name":"listed","score":0,"description":"in list.dnswl.org]"},{"name":"HTML_MESSAGE","score":0,"description":"BODY: HTML included in message"},{"name":"DKIM_VALID_AU","score":-0.1,"description":"Message has a valid DKIM or DK signature from author\'s"},{"name":"DKIM_SIGNED","score":0.1,"description":"Message has a DKIM or DK signature, not necessarily valid"},{"name":"DKIM_VALID","score":-0.1,"description":"Message has at least one valid DKIM or DK signature"},{"name":"RDNS_NONE","score":1.3,"description":"Delivered to internal network by a host with no rDNS"},{"name":"T_FILL_THIS_FORM_SHORT","score":0,"description":"Fill in a short form with personal information"}]},"dkim":{"signed":true,"valid":true},"email":"in-sales@m.foo.nar","tags":[],"sender":null,"template":null}}'
        })
        0 * errorCallback.receive(_)
        0 * snapshotCallback.receive(_)
        0 * reboundCallback.receive(_)
        0 * updateKeysCallback.receive(_)
        0 * httpReplyCallback.receive(_)
    }
}
