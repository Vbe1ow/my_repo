# Search engine
<h2>Локальный поисковый движок по сайту </h2>
<h3>Использованы техологии</h3>
<hr/>
Java 17, Spring boot, hibernate, liquibase, mysql, Spring data jpa, Apache lucene morphology, jsoup, maven

<h3>Описание проекта</h3>
<hr/>
После загрузки, поисковый движок, доступен по адресу http://localhost:8080
<h4>Web интерфейс</h4>
Возможности:<br>
- просмотр статуса интексации сайтов<br>
- обновления индекса страницы<br>
- обновление индекса всех сайтов<br>
<h4>Rest API</h4>
- GET /api/startIndexing - полная индексация(переиндексация) всх сайтов<br>
- GET /api/stopIndexing - остановка запущенной индексации<br>
- POST /api/indexPage - индексация отдельной страницы<br>
- GET /api/statistics -  получение статистики и служебной информации<br>
- GET /api/search - поиск страниц по переданному запросу<br>
<h4>Запуск проекта</h4>
Для запуска проекта в application.yaml укажите корректные параметры подключения к БД <br>
username:<br>
password:<br>
<hr/>
и список сайтов:<br>
indexing-settings:<br>
&emsp;sites:<br>
&emsp;&emsp;- url: https://www.lenta.ru<br>
&emsp;&emsp;name: Лента.ру<br>
<hr/>
