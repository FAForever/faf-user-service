import {html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';
import 'altcha';

@customElement("faf-altcha")
class FafAltcha extends LitElement {

    @property()
    token = ""
    @property()
    challengeUrl = ""

    protected createRenderRoot() {
        return this;
    }

    render() {
        return html`
            <altcha-widget challengeurl="${this.challengeUrl}"
                           @statechange="${this.handleStateChange}">
            </altcha-widget>
        `;
    }

    private handleStateChange(e: CustomEvent<{ state: string; payload: string }>) {
        this.token = e.detail.state === "verified" ? e.detail.payload : "";
        this.dispatchEvent(new Event("token-changed"));
    }
}
