package de.kieseltaucher.duplex.foreverybody.app.aws

import de.kieseltaucher.duplex.foreverybody.service.TestPDF
import groovy.json.JsonSlurper
import spock.lang.Specification

class AWSLambdaHandlerSpec extends Specification {

    def handler = new AWSLambdaHandler()
    def data = new TestPDF()

    def 'preconditions'() {
        when:
        doRequest()
        then:
        noExceptionThrown()

        expect:
        doRequest() != null
    }

    def 'when pdf is empty creates successful response'() {
        when:
        def response = doRequest()

        then:
        response.get('headers') == ['content-type': 'application/pdf']
        response.get('isBase64Encoded')
        pagesOfBody(response) == []
    }

    def 'sorts batch'() {
        when:
        data.addPage(1)
        data.addPage(3)
        data.addPage(2)
        def result = doRequest()
        then:
        pagesOfBody(result) == [1, 2, 3]
    }

    private Map doRequest() {
        def encodedData = Base64.getEncoder().encodeToString data.binary().bytes
        def httpApiRequest = '{"body": "' + encodedData + '"}'

        def out = new ByteArrayOutputStream()
        handler.handleRequest(new ByteArrayInputStream(httpApiRequest.bytes), out, new Object() as ContextStub)

        def resultReader = new InputStreamReader(new ByteArrayInputStream(out.toByteArray()))
        def parser = new JsonSlurper()
        def parsed = parser.parseText resultReader.text
        parsed as Map
    }

    private List<Integer> pagesOfBody(Map json) {
        def body = json.get('body') as String
        def pdf = new TestPDF()

        def loader = pdf.loader()
        loader.write Base64.getDecoder().decode(body)
        loader.close()

        pdf.pages()
    }

}
