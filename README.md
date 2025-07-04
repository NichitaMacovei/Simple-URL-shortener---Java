# Simple URL shortener - Java

A simple URL shortener that maps an URL address to a short code that is used to redirect to the orignal URL. 

- To create a new short code for a link: ```<URLShortener_url>/add?url=<your_url>```, example: ```localhost:8000/add?url=https://foobar.com/```
- To access a website via short code: ```<URLShortener_url>/<short_code>```, example: ```localhost:8000/foobar0```

**Important**: please note that ```?url``` value must be a complete link - e.g., ```localhost:8000/add?url=youtube.com``` will generate a short code, but the app will be unable to redirect to ```youtube.com```; however, short code for ```https://youtube.com/``` will redirect to YouTube as intended.
