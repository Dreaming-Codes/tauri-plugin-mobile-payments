<script>
    import Greet from './lib/Greet.svelte'
    import {startConnection, purchase} from 'tauri-plugin-mobile-payments-api'

    let response = ''

    function updateResponse(returnValue) {
        response += `[${new Date().toLocaleTimeString()}]` + (typeof returnValue === 'string' ? returnValue : JSON.stringify(returnValue)) + '<br>'
    }

    function _payment() {
        purchase({
            isSub: true,
            productId: 'com.example.product'
        })
    }

    function _startConnection() {
        startConnection().then((returnValue) => {
            updateResponse("Ok" + returnValue)
        }).catch((error) => {
            updateResponse("Error" + error)
        })
    }
</script>

<main class="container">
    <h1>Welcome to Tauri!</h1>

    <div class="row">
        <a href="https://vitejs.dev" target="_blank">
            <img src="/vite.svg" class="logo vite" alt="Vite Logo"/>
        </a>
        <a href="https://tauri.app" target="_blank">
            <img src="/tauri.svg" class="logo tauri" alt="Tauri Logo"/>
        </a>
        <a href="https://svelte.dev" target="_blank">
            <img src="/svelte.svg" class="logo svelte" alt="Svelte Logo"/>
        </a>
    </div>

    <p>
        Click on the Tauri, Vite, and Svelte logos to learn more.
    </p>

    <div class="row">
        <Greet/>
    </div>

    <div>
        <button on:click="{_startConnection}">Start connection</button>
        <button on:click="{_payment}">Start payment</button>
        <div>{@html response}</div>
    </div>

</main>

<style>
    .logo.vite:hover {
        filter: drop-shadow(0 0 2em #747bff);
    }

    .logo.svelte:hover {
        filter: drop-shadow(0 0 2em #ff3e00);
    }
</style>
