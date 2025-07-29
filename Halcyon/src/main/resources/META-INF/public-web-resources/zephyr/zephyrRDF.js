function ParseTTL(turtleData, store, baseURI) {
    try {
        $rdf.parse(turtleData, store, baseURI, 'text/turtle');
    } catch (err) {
        console.error('Error parsing Turtle:', err);       
    }
}

function DumpTTL(store, baseURI) {
    let serializedGraph = '';
    serializedGraph = $rdf.serialize(null, store, baseURI, 'text/turtle');
    console.log("--- Serialized Turtle Output ---");
    console.log(serializedGraph);
}

function ListElements2OLD(store, baseURI) {
    const RDF  = $rdf.Namespace('http://www.w3.org/1999/02/22-rdf-syntax-ns#');
    const zeph = $rdf.Namespace('https://halcyon.is/zephyr/ns/');
    const ZEPH_NS = zeph('').value;
    const typeQuads = store.match(null, RDF('type'), null);
    typeQuads.forEach(quad => {
        const obj = quad.object;
        if (obj.termType === 'NamedNode' && obj.value.startsWith(ZEPH_NS)) {
            console.log(`Subject <${quad.subject.value}> has type <${obj.value}>`);
        }
    });
}

function ListElements(store, baseURI) {
  const RDF  = $rdf.Namespace('http://www.w3.org/1999/02/22-rdf-syntax-ns#');
  const zeph = $rdf.Namespace('https://halcyon.is/zephyr/ns/');
  const ZEPH_NS = zeph('').value;
  return store
    .match(null, RDF('type'), null)
    .filter(quad =>
      quad.object.termType === 'NamedNode' &&
      quad.object.value.startsWith(ZEPH_NS)
    )
    .map(quad => ({
        subject: quad.subject,
        type:    quad.object
    }));
}

function ListImages(store, baseURI) {
    const foaf = $rdf.Namespace('http://xmlns.com/foaf/0.1/');
    const zeph = $rdf.Namespace('https://halcyon.is/zephyr/ns/');
    const stacks = store.match(null, $rdf.sym('http://www.w3.org/1999/02/22-rdf-syntax-ns#type'), zeph('Stack'));
    stacks.forEach(stack => {
        const layers = store.match(stack.subject, zeph('layers'), null);
        const layerList = layers[0].object.elements;
        layerList.forEach(layerName => {
            const image = store.match(layerName, zeph('src'), null);
            console.log("Image : "+ image[0].object.value);     
        });
    });
}

export { ParseTTL, DumpTTL, ListElements };
