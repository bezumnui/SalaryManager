# v0.0.1
Allows managing of salaries.
Online players get paid their salaries every set amound of time (1200s=20m by default).
If a player is in multiple groups (and/or has themselves a personal salary), they get the most profitable one of the selection (you can change this in the config to allowe them to get the sum).


`/salary <group or player> <amount>` - set a salary of a group or a player

`/salarypay [nr]` - pay the number of times right away 

`/salarylist <-p or -g> [page]` - list group or player salaries (6 per page)

`/salarysetperiod [period]` - set a new period between payments

`/salarynext` - check when you're next getting paid

`/salaryreload` - reload the config

### Commands and permissions
```
Command             	Permission              	Description                             
salary              	salarymanager.set       	Set a salary to a player or group       
salarypay           	salarymanager.pay       	Pay all online players                  
salarylist          	salarymanager.list      	List salaries of groups or players      
salarysetperiod     	salarymanager.setperiod 	Set the period in seconds               
salarynext          	salarymanager.next      	When is your next payday                
salaryreload        	salarymanager.reload    	Reload the config                       
```
### Extra permissions
```
Permission              	Children                                        	Descrpition                             
salarymanager.set       	None                                            	Allow seting of salaries                
salarymanager.pay       	None                                            	Allow paying of salaries                
salarymanager.get       	None                                            	Allow seting of salaries                
salarymanager.reload    	None                                            	Allow reloading the config              
salarymanager.list      	None                                            	Allow listing the salaries              
salarymanager.setperiod 	None                                            	Allow setting of the period             
salarymanager.notify    	None                                            	Get notified of payments same (totals and nr of people getting paid)
```
