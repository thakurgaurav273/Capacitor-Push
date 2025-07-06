import { CapacitorPush } from 'capacitor-push';

window.testEcho = () => {
    const inputValue = document.getElementById("echoInput").value;
    CapacitorPush.echo({ value: inputValue })
}
