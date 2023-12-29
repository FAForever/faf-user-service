import {html, LitElement} from 'lit';
import {customElement, property} from 'lit/decorators.js';

@customElement("faf-recaptcha")
class FafRecaptcha extends LitElement {

    @property()
    token = ""
    @property()
    siteKey = ""

    private recaptchaScript = document.createElement("script")

    constructor() {
        super();
        const globalWindow = window as any;
        globalWindow.recaptchaCallback = (token: string) => {
            this.token = token;
            this.dispatchEvent(new Event("token-changed"));
        }
        globalWindow.recaptchaExpiredCallback = () => {
            this.token = ""
            this.dispatchEvent(new CustomEvent("token-changed"));
        }
        this.recaptchaScript.src = "https://www.google.com/recaptcha/api.js";
    }

    protected createRenderRoot() {
        return this;
    }

    render() {
        return html`
            <div class="g-recaptcha" data-sitekey="${this.siteKey}" data-callback="recaptchaCallback"
                 data-expired-callback="recaptchaExpiredCallback"></div>
            ${this.recaptchaScript}
        `;
    }
}