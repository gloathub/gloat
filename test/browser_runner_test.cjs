const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');

const page = fs.readFileSync('template/index.html', 'utf8');
const scripts = [...page.matchAll(/<script>([\s\S]*?)<\/script>/g)];
const runner = scripts.at(-1)[1]
  .replace('PROGRAM-NAME-JSON', JSON.stringify('program'));

function loadRunner(search) {
  const output = {children: [], appendChild(row) {
    this.children.push(row);
  }};
  const context = {
    Error,
    Promise,
    String,
    TextDecoder,
    Uint8Array,
    console: {log() {}},
    document: {
      getElementById() { return output; },
      createElement() { return {className: '', textContent: ''}; },
    },
    fetch() { return new Promise(() => {}); },
    fs: {writeSync(_fd, buffer) { return buffer.length; }},
    location: {search},
  };
  context.globalThis = context;
  context.Go = class {
    constructor() {
      this.importObject = {};
      context.go = this;
    }
  };
  vm.runInNewContext(runner, context);
  return {context, output};
}

const {context, output} = loadRunner('?one%2Ctwo,three');
assert.deepEqual(Array.from(context.go.argv), [
  'program',
  'one,two',
  'three',
]);

const encoder = new TextEncoder();
context.fs.writeSync(1, encoder.encode('out'));
context.fs.writeSync(2, encoder.encode('error\n'));
context.fs.writeSync(1, encoder.encode('put\n'));

assert.equal(output.children.length, 2);
assert.equal(output.children[0].className, 'output-row stdout');
assert.equal(output.children[0].textContent, 'output');
assert.equal(output.children[1].className, 'output-row stderr');
assert.equal(output.children[1].textContent, 'error');

context.console.log('browser message');
assert.equal(output.children.length, 2);

const malformed = loadRunner('?bad%2');
assert.equal(malformed.output.children.length, 1);
assert.equal(malformed.output.children[0].className, 'output-row stderr');
assert.match(
  malformed.output.children[0].textContent,
  /^Error: (URIError: )?URI malformed/,
);
