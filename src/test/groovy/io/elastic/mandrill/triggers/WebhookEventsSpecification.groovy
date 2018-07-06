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
                        .withBody(equalToIgnoringCase('{"key":"super-secret","url":"http://example/webhook-url","events":["send","open","click"]}'), "application/json"),
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

        def mandrillEvents = '[{"event":"inbound","ts":1503928695,"msg":{"raw_msg":"Received:+from+mail-wr0-f177.google.com+(unknown+[209.85.128.177])\\n\\tby+relay-3.us-west-2.relay-prod+(Postfix)+with+ESMTPS+id+5CFE72002BA\\n\\tfor+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+13:58:23++0000+(UTC)\\nReceived:+by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5\\n++++++++for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+06:58:23+-0700+(PDT)\\nDKIM-Signature:+v=1;+a=rsa-sha256;+c=relaxed\\/relaxed;\\n++++++++d=foo.nar;+s=google;\\n++++++++h=mime-version:in-reply-to:references:from:date:message-id:subject:to;\\n++++++++bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau\\/FhNuT0\\/FHbhI=;\\n++++++++b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ\\/dDDJMJrYLL02Kz1CZi\\n+++++++++50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2\\n+++++++++aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=\\nX-Google-DKIM-Signature:+v=1;+a=rsa-sha256;+c=relaxed\\/relaxed;\\n++++++++d=1e100.net;+s=20161025;\\n++++++++h=x-gm-message-state:mime-version:in-reply-to:references:from:date\\n+++++++++:message-id:subject:to;\\n++++++++bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau\\/FhNuT0\\/FHbhI=;\\n++++++++b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0\\n+++++++++hwCixy3k7sX3cvSCVscyV9sk8\\/i\\/ICZjZuhDsOI\\/3IaM+DDy\\/HvU\\/zSK0xJuW8hUtcqB\\n+++++++++Za\\/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR\\n+++++++++uPdiiDZZrJt706rjRwSDsa5Yx\\/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA\\n+++++++++1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk\\n+++++++++fXbg==\\nX-Gm-Message-State:+AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE\\/4OayVN4HIeJ20aFa9Mm5ui\\n\\trMkaN\\/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==\\nX-Received:+by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697;\\n+Mon,+28+Aug+2017+06:58:21+-0700+(PDT)\\nMIME-Version:+1.0\\nReceived:+by+10.223.198.9+with+HTTP;+Mon,+28+Aug+2017+06:58:01+-0700+(PDT)\\nX-Originating-IP:+[84.163.64.106]\\nIn-Reply-To:+<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nReferences:+<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nFrom:+John+Snow+<john.snow@foo.nar>\\nDate:+Mon,+28+Aug+2017+15:58:01++0200\\nMessage-ID:+<CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>\\nSubject:+Fwd:+New+Lead+from+Trial+Request+Page\\nTo:+in-sales@m.foo.nar\\nContent-Type:+multipart\\/alternative;+boundary=\\"089e0824170c0856720557d0b076\\"\\n\\n--089e0824170c0856720557d0b076\\nContent-Type:+text\\/plain;+charset=\\"UTF-8\\"\\n\\nNew+lead+has+been+captured+from+the+Trial+Request+page!\\n\\nLead\'s+name:+John+Doe\\nContact+email:+johnd@HBO.cz\\nContact+phone+number:\\nPosition:+Network+specialist\\nCompany:+HBO+Group+s.r.o\\nCompany+Size:+1+-+10\\n\\nSubmitted+message:\\nEmails+and+MicrosoftSQL+...\\n\\n\\n---------\\nDebug+Information+about+Submitter:\\n\\nIP+address+is:+84.42.179.2\\nUser+Agent+is:+Mozilla\\/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit\\/537.36\\n(KHTML,+like+Gecko)+Chrome\\/60.0.3112.101+Safari\\/537.36\\nPage+URL+is:+++https:\\/\\/www.foo.nar\\/request-trial\\/\\nSubmission+Date+is:+25\\/08\\/2017\\nSubmission+Time+is:+11:54\\nHidden+field+called+ga_ID+has+value:+2055698932.1503652393\\n\\n--089e0824170c0856720557d0b076\\nContent-Type:+text\\/html;+charset=\\"UTF-8\\"\\nContent-Transfer-Encoding:+quoted-printable\\n\\n<div+dir=3D\\"ltr\\"><div+class=3D\\"gmail_quote\\">New+lead+has+been+captured+from=\\n+the+Trial+Request+page!<br>\\n<br>\\nLead&#39;s+name:+John+Doe<br>\\nContact+email:+<a+href=3D\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz<\\/a><b=\\nr>\\nContact+phone+number:<br>\\nPosition:+Network+specialist<br>\\nCompany:+HBO+Group+s.r.o<br>\\nCompany+Size:+1+-+10<br>\\n<br>\\nSubmitted+message:<br>\\nEmails+and+MicrosoftSQL+...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug+Information+about+Submitter:<br>\\n<br>\\nIP+address+is:+84.42.179.2<br>\\nUser+Agent+is:+Mozilla\\/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit\\/537.36=\\n+(KHTML,+like+Gecko)+Chrome\\/60.0.3112.101+Safari\\/537.36<br>\\nPage+URL+is:=C2=A0+=C2=A0<a+href=3D\\"https:\\/\\/www.foo.nar\\/request-trial\\/\\"+=\\nrel=3D\\"noreferrer\\"+target=3D\\"_blank\\">https:\\/\\/www.foo.nar\\/<wbr>request-tr=\\nial\\/<\\/a><br>\\nSubmission+Date+is:+25\\/08\\/2017<br>\\nSubmission+Time+is:+11:54<br>\\nHidden+field+called+ga_ID+has+value:+<a+href=3D\\"tel:2055698932\\"+value=3D\\"+1=\\n2055698932\\">2055698932<\\/a>.1503652393<br>\\n<\\/div><br><\\/div>\\n\\n--089e0824170c0856720557d0b076--","headers":{"Received":["from+mail-wr0-f177.google.com+(unknown+[209.85.128.177])+by+relay-3.us-west-2.relay-prod+(Postfix)+with+ESMTPS+id+5CFE72002BA+for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+13:58:23++0000+(UTC)","by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5+for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+06:58:23+-0700+(PDT)","by+10.223.198.9+with+HTTP;+Mon,+28+Aug+2017+06:58:01+-0700+(PDT)"],"Dkim-Signature":"v=1;+a=rsa-sha256;+c=relaxed\\/relaxed;+d=foo.nar;+s=google;+h=mime-version:in-reply-to:references:from:date:message-id:subject:to;+bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau\\/FhNuT0\\/FHbhI=;+b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ\\/dDDJMJrYLL02Kz1CZi+50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2+aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=","X-Google-Dkim-Signature":"v=1;+a=rsa-sha256;+c=relaxed\\/relaxed;+d=1e100.net;+s=20161025;+h=x-gm-message-state:mime-version:in-reply-to:references:from:date+:message-id:subject:to;+bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau\\/FhNuT0\\/FHbhI=;+b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0+hwCixy3k7sX3cvSCVscyV9sk8\\/i\\/ICZjZuhDsOI\\/3IaM+DDy\\/HvU\\/zSK0xJuW8hUtcqB+Za\\/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR+uPdiiDZZrJt706rjRwSDsa5Yx\\/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA+1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk+fXbg==","X-Gm-Message-State":"AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE\\/4OayVN4HIeJ20aFa9Mm5ui+rMkaN\\/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==","X-Received":"by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697;+Mon,+28+Aug+2017+06:58:21+-0700+(PDT)","Mime-Version":"1.0","X-Originating-Ip":"[84.163.64.106]","In-Reply-To":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","References":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","From":"John+Snow+<john.snow@foo.nar>","Date":"Mon,+28+Aug+2017+15:58:01++0200","Message-Id":"<CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>","Subject":"Fwd:+New+Lead+from+Trial+Request+Page","To":"in-sales@m.foo.nar","Content-Type":"multipart\\/alternative;+boundary=\\"089e0824170c0856720557d0b076\\""},"text":"New+lead+has+been+captured+from+the+Trial+Request+page!\\n\\nLead\'s+name:+John+Doe\\nContact+email:+johnd@HBO.cz\\nContact+phone+number:\\nPosition:+Network+specialist\\nCompany:+HBO+Group+s.r.o\\nCompany+Size:+1+-+10\\n\\nSubmitted+message:\\nEmails+and+MicrosoftSQL+...\\n\\n\\n---------\\nDebug+Information+about+Submitter:\\n\\nIP+address+is:+84.42.179.2\\nUser+Agent+is:+Mozilla\\/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit\\/537.36\\n(KHTML,+like+Gecko)+Chrome\\/60.0.3112.101+Safari\\/537.36\\nPage+URL+is:+++https:\\/\\/www.foo.nar\\/request-trial\\/\\nSubmission+Date+is:+25\\/08\\/2017\\nSubmission+Time+is:+11:54\\nHidden+field+called+ga_ID+has+value:+2055698932.1503652393\\n\\n","text_flowed":false,"html":"<div+dir=\\"ltr\\"><div+class=\\"gmail_quote\\">New+lead+has+been+captured+from+the+Trial+Request+page!<br>\\n<br>\\nLead&#39;s+name:+John+Doe<br>\\nContact+email:+<a+href=\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz<\\/a><br>\\nContact+phone+number:<br>\\nPosition:+Network+specialist<br>\\nCompany:+HBO+Group+s.r.o<br>\\nCompany+Size:+1+-+10<br>\\n<br>\\nSubmitted+message:<br>\\nEmails+and+MicrosoftSQL+...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug+Information+about+Submitter:<br>\\n<br>\\nIP+address+is:+84.42.179.2<br>\\nUser+Agent+is:+Mozilla\\/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit\\/537.36+(KHTML,+like+Gecko)+Chrome\\/60.0.3112.101+Safari\\/537.36<br>\\nPage+URL+is:\\u00a0+\\u00a0<a+href=\\"https:\\/\\/www.foo.nar\\/request-trial\\/\\"+rel=\\"noreferrer\\"+target=\\"_blank\\">https:\\/\\/www.foo.nar\\/<wbr>request-trial\\/<\\/a><br>\\nSubmission+Date+is:+25\\/08\\/2017<br>\\nSubmission+Time+is:+11:54<br>\\nHidden+field+called+ga_ID+has+value:+<a+href=\\"tel:2055698932\\"+value=\\"+12055698932\\">2055698932<\\/a>.1503652393<br>\\n<\\/div><br><\\/div>\\n\\n","from_email":"john.snow@foo.nar","from_name":"John+Snow","to":[["in-sales@m.foo.nar",null]],"subject":"Fwd:+New+Lead+from+Trial+Request+Page","spf":{"result":"pass","detail":"sender+SPF+authorized"},"spam_report":{"score":1.2,"matched_rules":[{"name":"URIBL_BLOCKED","score":0,"description":"ADMINISTRATOR+NOTICE:+The+query+to+URIBL+was+blocked."},{"name":null,"score":0,"description":null},{"name":"more","score":0,"description":"information."},{"name":"HBO.cz]","score":0,"description":null},{"name":"RCVD_IN_DNSWL_NONE","score":-0,"description":"RBL:+Sender+listed+at+http:\\/\\/www.dnswl.org\\/,+no"},{"name":"listed","score":0,"description":"in+list.dnswl.org]"},{"name":"HTML_MESSAGE","score":0,"description":"BODY:+HTML+included+in+message"},{"name":"DKIM_VALID_AU","score":-0.1,"description":"Message+has+a+valid+DKIM+or+DK+signature+from+author\'s"},{"name":"DKIM_SIGNED","score":0.1,"description":"Message+has+a+DKIM+or+DK+signature,+not+necessarily+valid"},{"name":"DKIM_VALID","score":-0.1,"description":"Message+has+at+least+one+valid+DKIM+or+DK+signature"},{"name":"RDNS_NONE","score":1.3,"description":"Delivered+to+internal+network+by+a+host+with+no+rDNS"},{"name":"T_FILL_THIS_FORM_SHORT","score":0,"description":"Fill+in+a+short+form+with+personal+information"}]},"dkim":{"signed":true,"valid":true},"email":"in-sales@m.foo.nar","tags":[],"sender":null,"template":null}}]'

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
            JSON.stringify(it.getBody()) == '{"event":"inbound","ts":1503928695,"msg":{"raw_msg":"Received:+from+mail-wr0-f177.google.com+(unknown+[209.85.128.177])\\n\\tby+relay-3.us-west-2.relay-prod+(Postfix)+with+ESMTPS+id+5CFE72002BA\\n\\tfor+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+13:58:23++0000+(UTC)\\nReceived:+by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5\\n++++++++for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+06:58:23+-0700+(PDT)\\nDKIM-Signature:+v=1;+a=rsa-sha256;+c=relaxed/relaxed;\\n++++++++d=foo.nar;+s=google;\\n++++++++h=mime-version:in-reply-to:references:from:date:message-id:subject:to;\\n++++++++bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;\\n++++++++b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ/dDDJMJrYLL02Kz1CZi\\n+++++++++50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2\\n+++++++++aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=\\nX-Google-DKIM-Signature:+v=1;+a=rsa-sha256;+c=relaxed/relaxed;\\n++++++++d=1e100.net;+s=20161025;\\n++++++++h=x-gm-message-state:mime-version:in-reply-to:references:from:date\\n+++++++++:message-id:subject:to;\\n++++++++bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;\\n++++++++b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0\\n+++++++++hwCixy3k7sX3cvSCVscyV9sk8/i/ICZjZuhDsOI/3IaM+DDy/HvU/zSK0xJuW8hUtcqB\\n+++++++++Za/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR\\n+++++++++uPdiiDZZrJt706rjRwSDsa5Yx/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA\\n+++++++++1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk\\n+++++++++fXbg==\\nX-Gm-Message-State:+AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE/4OayVN4HIeJ20aFa9Mm5ui\\n\\trMkaN/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==\\nX-Received:+by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697;\\n+Mon,+28+Aug+2017+06:58:21+-0700+(PDT)\\nMIME-Version:+1.0\\nReceived:+by+10.223.198.9+with+HTTP;+Mon,+28+Aug+2017+06:58:01+-0700+(PDT)\\nX-Originating-IP:+[84.163.64.106]\\nIn-Reply-To:+<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nReferences:+<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>\\nFrom:+John+Snow+<john.snow@foo.nar>\\nDate:+Mon,+28+Aug+2017+15:58:01++0200\\nMessage-ID:+<CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>\\nSubject:+Fwd:+New+Lead+from+Trial+Request+Page\\nTo:+in-sales@m.foo.nar\\nContent-Type:+multipart/alternative;+boundary=\\"089e0824170c0856720557d0b076\\"\\n\\n--089e0824170c0856720557d0b076\\nContent-Type:+text/plain;+charset=\\"UTF-8\\"\\n\\nNew+lead+has+been+captured+from+the+Trial+Request+page!\\n\\nLead\'s+name:+John+Doe\\nContact+email:+johnd@HBO.cz\\nContact+phone+number:\\nPosition:+Network+specialist\\nCompany:+HBO+Group+s.r.o\\nCompany+Size:+1+-+10\\n\\nSubmitted+message:\\nEmails+and+MicrosoftSQL+...\\n\\n\\n---------\\nDebug+Information+about+Submitter:\\n\\nIP+address+is:+84.42.179.2\\nUser+Agent+is:+Mozilla/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit/537.36\\n(KHTML,+like+Gecko)+Chrome/60.0.3112.101+Safari/537.36\\nPage+URL+is:+++https://www.foo.nar/request-trial/\\nSubmission+Date+is:+25/08/2017\\nSubmission+Time+is:+11:54\\nHidden+field+called+ga_ID+has+value:+2055698932.1503652393\\n\\n--089e0824170c0856720557d0b076\\nContent-Type:+text/html;+charset=\\"UTF-8\\"\\nContent-Transfer-Encoding:+quoted-printable\\n\\n<div+dir=3D\\"ltr\\"><div+class=3D\\"gmail_quote\\">New+lead+has+been+captured+from=\\n+the+Trial+Request+page!<br>\\n<br>\\nLead&#39;s+name:+John+Doe<br>\\nContact+email:+<a+href=3D\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz</a><b=\\nr>\\nContact+phone+number:<br>\\nPosition:+Network+specialist<br>\\nCompany:+HBO+Group+s.r.o<br>\\nCompany+Size:+1+-+10<br>\\n<br>\\nSubmitted+message:<br>\\nEmails+and+MicrosoftSQL+...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug+Information+about+Submitter:<br>\\n<br>\\nIP+address+is:+84.42.179.2<br>\\nUser+Agent+is:+Mozilla/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit/537.36=\\n+(KHTML,+like+Gecko)+Chrome/60.0.3112.101+Safari/537.36<br>\\nPage+URL+is:=C2=A0+=C2=A0<a+href=3D\\"https://www.foo.nar/request-trial/\\"+=\\nrel=3D\\"noreferrer\\"+target=3D\\"_blank\\">https://www.foo.nar/<wbr>request-tr=\\nial/</a><br>\\nSubmission+Date+is:+25/08/2017<br>\\nSubmission+Time+is:+11:54<br>\\nHidden+field+called+ga_ID+has+value:+<a+href=3D\\"tel:2055698932\\"+value=3D\\"+1=\\n2055698932\\">2055698932</a>.1503652393<br>\\n</div><br></div>\\n\\n--089e0824170c0856720557d0b076--","headers":{"Received":["from+mail-wr0-f177.google.com+(unknown+[209.85.128.177])+by+relay-3.us-west-2.relay-prod+(Postfix)+with+ESMTPS+id+5CFE72002BA+for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+13:58:23++0000+(UTC)","by+mail-wr0-f177.google.com+with+SMTP+id+40so1418592wrv.5+for+<in-sales@m.foo.nar>;+Mon,+28+Aug+2017+06:58:23+-0700+(PDT)","by+10.223.198.9+with+HTTP;+Mon,+28+Aug+2017+06:58:01+-0700+(PDT)"],"Dkim-Signature":"v=1;+a=rsa-sha256;+c=relaxed/relaxed;+d=foo.nar;+s=google;+h=mime-version:in-reply-to:references:from:date:message-id:subject:to;+bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;+b=WPXDdDI3QADFr9pHjoJRg86Oz5XvFoBjj199BdjDQZDyHyJ/dDDJMJrYLL02Kz1CZi+50FJG31E0rci+8tHyJaDqaHkp6yAlJERjIux5elAb4JppEFp317rMUHPU1SDYJWIVTz2+aq2SSBnhQjNSKFi8JziMNJoxOeDgyRnpDqCYw=","X-Google-Dkim-Signature":"v=1;+a=rsa-sha256;+c=relaxed/relaxed;+d=1e100.net;+s=20161025;+h=x-gm-message-state:mime-version:in-reply-to:references:from:date+:message-id:subject:to;+bh=YxAgQTheXoVRva5xHCxk5ez+BWqEau/FhNuT0/FHbhI=;+b=Qm2COgxxBzlOvfs4FAbpigaaZTn7rkaaZ2pwmeQD0U1Vj4gHchm9mQ7orejFHDXKV0+hwCixy3k7sX3cvSCVscyV9sk8/i/ICZjZuhDsOI/3IaM+DDy/HvU/zSK0xJuW8hUtcqB+Za/3GL3vfhdDn1j7Qj4PEka07tjQih1HBhJ+Dxy9h3MhBiTdTMMn0F4KPxvfVnJBdAMR+uPdiiDZZrJt706rjRwSDsa5Yx/i+N3IB31bM5hId8oKVVJTDSzLN+QCnneK3OO63jEuA+1MN71XxIuhMNPFbV1Pdj0Flfx7Quag+nP3rpyQXoF0MCkD7NXHUIQmNmA4N50DCsUlOk+fXbg==","X-Gm-Message-State":"AHYfb5jCm7Tg83ya2vVoAinHuOOHDyYcE/4OayVN4HIeJ20aFa9Mm5ui+rMkaN/lLOFJgPUpFmDQCBsRceN3n3ZmMUPAcYg==","X-Received":"by+10.223.197.133+with+SMTP+id+m5mr558062wrg.276.1503928701697;+Mon,+28+Aug+2017+06:58:21+-0700+(PDT)","Mime-Version":"1.0","X-Originating-Ip":"[84.163.64.106]","In-Reply-To":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","References":"<9196e4ebe211e67e995df6faba3b111e@www.foo.nar>","From":"John+Snow+<john.snow@foo.nar>","Date":"Mon,+28+Aug+2017+15:58:01++0200","Message-Id":"<CA+Lhcb8J6dO=1r0QdthBvLA2_1OXzEmKDd19OhF2SLpxRDY25A@mail.gmail.com>","Subject":"Fwd:+New+Lead+from+Trial+Request+Page","To":"in-sales@m.foo.nar","Content-Type":"multipart/alternative;+boundary=\\"089e0824170c0856720557d0b076\\""},"text":"New+lead+has+been+captured+from+the+Trial+Request+page!\\n\\nLead\'s+name:+John+Doe\\nContact+email:+johnd@HBO.cz\\nContact+phone+number:\\nPosition:+Network+specialist\\nCompany:+HBO+Group+s.r.o\\nCompany+Size:+1+-+10\\n\\nSubmitted+message:\\nEmails+and+MicrosoftSQL+...\\n\\n\\n---------\\nDebug+Information+about+Submitter:\\n\\nIP+address+is:+84.42.179.2\\nUser+Agent+is:+Mozilla/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit/537.36\\n(KHTML,+like+Gecko)+Chrome/60.0.3112.101+Safari/537.36\\nPage+URL+is:+++https://www.foo.nar/request-trial/\\nSubmission+Date+is:+25/08/2017\\nSubmission+Time+is:+11:54\\nHidden+field+called+ga_ID+has+value:+2055698932.1503652393\\n\\n","text_flowed":false,"html":"<div+dir=\\"ltr\\"><div+class=\\"gmail_quote\\">New+lead+has+been+captured+from+the+Trial+Request+page!<br>\\n<br>\\nLead&#39;s+name:+John+Doe<br>\\nContact+email:+<a+href=\\"mailto:johnd@HBO.cz\\">johnd@HBO.cz</a><br>\\nContact+phone+number:<br>\\nPosition:+Network+specialist<br>\\nCompany:+HBO+Group+s.r.o<br>\\nCompany+Size:+1+-+10<br>\\n<br>\\nSubmitted+message:<br>\\nEmails+and+MicrosoftSQL+...<br>\\n<br>\\n<br>\\n---------<br>\\nDebug+Information+about+Submitter:<br>\\n<br>\\nIP+address+is:+84.42.179.2<br>\\nUser+Agent+is:+Mozilla/5.0+(Windows+NT+10.0;+Win64;+x64)+AppleWebKit/537.36+(KHTML,+like+Gecko)+Chrome/60.0.3112.101+Safari/537.36<br>\\nPage+URL+is: + <a+href=\\"https://www.foo.nar/request-trial/\\"+rel=\\"noreferrer\\"+target=\\"_blank\\">https://www.foo.nar/<wbr>request-trial/</a><br>\\nSubmission+Date+is:+25/08/2017<br>\\nSubmission+Time+is:+11:54<br>\\nHidden+field+called+ga_ID+has+value:+<a+href=\\"tel:2055698932\\"+value=\\"+12055698932\\">2055698932</a>.1503652393<br>\\n</div><br></div>\\n\\n","from_email":"john.snow@foo.nar","from_name":"John+Snow","to":[["in-sales@m.foo.nar",null]],"subject":"Fwd:+New+Lead+from+Trial+Request+Page","spf":{"result":"pass","detail":"sender+SPF+authorized"},"spam_report":{"score":1.2,"matched_rules":[{"name":"URIBL_BLOCKED","score":0,"description":"ADMINISTRATOR+NOTICE:+The+query+to+URIBL+was+blocked."},{"name":null,"score":0,"description":null},{"name":"more","score":0,"description":"information."},{"name":"HBO.cz]","score":0,"description":null},{"name":"RCVD_IN_DNSWL_NONE","score":0,"description":"RBL:+Sender+listed+at+http://www.dnswl.org/,+no"},{"name":"listed","score":0,"description":"in+list.dnswl.org]"},{"name":"HTML_MESSAGE","score":0,"description":"BODY:+HTML+included+in+message"},{"name":"DKIM_VALID_AU","score":-0.1,"description":"Message+has+a+valid+DKIM+or+DK+signature+from+author\'s"},{"name":"DKIM_SIGNED","score":0.1,"description":"Message+has+a+DKIM+or+DK+signature,+not+necessarily+valid"},{"name":"DKIM_VALID","score":-0.1,"description":"Message+has+at+least+one+valid+DKIM+or+DK+signature"},{"name":"RDNS_NONE","score":1.3,"description":"Delivered+to+internal+network+by+a+host+with+no+rDNS"},{"name":"T_FILL_THIS_FORM_SHORT","score":0,"description":"Fill+in+a+short+form+with+personal+information"}]},"dkim":{"signed":true,"valid":true},"email":"in-sales@m.foo.nar","tags":[],"sender":null,"template":null}}'
        })
        0 * errorCallback.receive(_)
        0 * snapshotCallback.receive(_)
        0 * reboundCallback.receive(_)
        0 * updateKeysCallback.receive(_)
        0 * httpReplyCallback.receive(_)
    }
}
